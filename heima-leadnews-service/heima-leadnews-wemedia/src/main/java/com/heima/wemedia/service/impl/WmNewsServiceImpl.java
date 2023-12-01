package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vo.WmNewsVo;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import com.heima.wemedia.service.WmUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 根据条件查询列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {

        //检验参数
        dto.checkParam();

        //条件分页查询:状态、关键字、频道、日期、用户
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        //根据当前用户信息查询数据
        queryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        //根据状态查询
        if (dto.getStatus() != null) {
            queryWrapper.eq(WmNews::getStatus, dto.getStatus());
        }
        //根据频道查询
        if (dto.getChannelId() != null) {
            queryWrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }
        //根据关键字模糊查询
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            queryWrapper.like(WmNews::getTitle, dto.getKeyword());
        }
        //根据日期查询
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
            queryWrapper.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        }
        //根据发布时间倒序排序
        queryWrapper.orderByDesc(WmNews::getPublishTime);


        //返回数据
        page = page(page, queryWrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    /**
     * 修改发布文章或者保存为草稿
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {

        //参数校验
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //保存或者修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝,属性名和类型都相同时才能拷贝
        BeanUtils.copyProperties(dto, wmNews);
        //封面图片
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        //如果封面类型为-1
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }

        saveOrUpdateWmNews(wmNews);


        //判断是否为草稿
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        //不是草稿，保存文章内容与素材的关系
        List<String> materials = extractUrlInfo(dto.getContent());
        saveMaterialInfoForContent(materials, wmNews.getId());

        //保存文章封面图片与素材的关系
        saveRelationInfoForCover(dto, wmNews, materials);

        //进行审核
        //wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 保存文章封面与素材的关系
     * <p>
     * 功能1：如果当前封面类型是自动，则设置封面类型的数据
     * <p>
     * 功能2：保存封面图片与素材的关系
     *
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelationInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();

        //如果当前封面类型是自动
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            //多图
            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            }
            //单图
            else if (materials.size() >= 1 && materials.size() < 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }
            //无图
            else {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            //修改文章
            if (images != null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images, ","));
            }

            updateById(wmNews);
        }

        if (images != null && images.size() > 0) {
            saveRelationInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }

    }

    /**
     * 处理文章内容图片与素材的关系
     *
     * @param materials
     * @param newsId
     */
    private void saveMaterialInfoForContent(List<String> materials, Integer newsId) {
        saveRelationInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }


    /**
     * 保存文章图片与素材关系到数据库中
     *
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelationInfo(List<String> materials, Integer newsId, Short type) {
        if (materials != null && !materials.isEmpty()) {
            List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));

            //判断素材是否有效
            if (wmMaterials == null || wmMaterials.size() == 0) {
                //手动抛出异常，进行数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FALL);
            }

            //当查询的数据个数与传过来的素材个数不一样
            if (wmMaterials.size() != materials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FALL);
            }

            //获取对应的id
            List<Integer> materialsId = wmMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            //进行批量保存
            wmNewsMaterialMapper.saveRelations(materialsId, newsId, type);
        }
    }

    /**
     * 提取content中的图片url
     *
     * @param content
     */
    private List<String> extractUrlInfo(String content) {
        List<String> result = new ArrayList<>();

        List<Map> materials = JSON.parseArray(content, Map.class);
        for (Map map : materials) {
            if (map.get("type").equals("image")) {
                String url = (String) map.get("value");
                result.add(url);
            }
        }

        return result;
    }

    /**
     * 保存或者修改文章
     *
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1);

        if (wmNews.getId() == null) {
            save(wmNews);
        } else {
            //删除之前与素材的关系
            LambdaQueryWrapper<WmNewsMaterial> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WmNewsMaterial::getNewsId, wmNews.getId());
            wmNewsMaterialMapper.delete(queryWrapper);

            updateById(wmNews);
        }

    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 文章上下架
     *
     * @param wmNewsDto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto wmNewsDto) {

        //检验参数
        if (wmNewsDto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询文章
        WmNews wmNews = getById(wmNewsDto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文章不存在");
        }

        //判断文章是否已经发布
        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前文章不是发布状态，不能进行文章上下架");
        }

        //修改文章enbale
        if (wmNewsDto.getEnable() != null && wmNewsDto.getEnable() > -1 && wmNewsDto.getEnable() < 2) {
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable, wmNewsDto.getEnable())
                    .eq(WmNews::getId, wmNewsDto.getId())
            );
            if (wmNews.getArticleId() != null) {
                //发送消息，通知Article修改文章配置
                Map<String, Object> map = new HashMap<>();
                map.put("articleId", wmNews.getArticleId());
                map.put("enable", wmNewsDto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(map));
            }
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 文章列表查询（需要验证的）
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult listVo(NewsAuthDto dto) {

        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        page = page(page, Wrappers.<WmNews>lambdaQuery()
                .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                .eq(StringUtils.isNotBlank(dto.getTitle()), WmNews::getTitle, dto.getTitle())
        );

        //参数赋值
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    private WmUserService wmUserService;

    /**
     * 查看文章详情
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult newsDetail(Integer id) {

        //参数校验
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询文章
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }

        //查询作者
        WmUser wmUser = wmUserService.getById(wmNews.getUserId());
        WmNewsVo wmNewsVo = new WmNewsVo();
        if (wmNewsVo != null) {
            wmNewsVo.setAuthorName(wmUser.getName());
        }

        //进行赋值
        BeanUtils.copyProperties(wmNews, wmNewsVo);

        return ResponseResult.okResult(wmNewsVo);
    }

    /**
     * 通过审核
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDto dto) {

        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询文章信息
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }


        //更新
        wmNews.setStatus(dto.getStatus().shortValue());
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);
/*        update(Wrappers.<WmNews>lambdaUpdate()
                .set(WmNews::getStatus, status)
                .set(StringUtils.isNotBlank(dto.getMsg()), WmNews::getReason, dto.getMsg())
                .eq(dto.getId() != null, WmNews::getId, dto.getId())
                .eq(WmNews::getStatus, 3)
        );*/

        //审核成功
        if(status.equals(WemediaConstants.WM_NEWS_PASS)){
            //创建App文章
            ResponseResult responseResult = wmNewsAutoScanService.saveAppArticle(wmNews);
            if(responseResult.getCode().equals(200)){
                wmNews.setArticleId((Long) responseResult.getData());
                wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
                updateById(wmNews);
            }
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}