package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.itheima.apis.article.IArticleClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 审核文章
     *
     * @param id
     */
    @Override
    public void autoScanWmNews(Integer id) {
        //参数校验
        WmNews wmNews = wmNewsMapper.selectOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl- 文章不存在");
        }

        //进行判断
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            //从文章中提取文章内容、图片（封面、内容图片）
            Map<String, Object> imageAndContent = extractImageAndContent(wmNews);

            //通过阿里云内容安全服务进行审核
            //审核文本内容
            //Boolean flag = handleTextScan((String) imageAndContent.get("content"), wmNews);
            Boolean flag=true;
            if (!flag) {
                return;
            }

            //审核图片
            //flag = handleImageScan((List<String>) imageAndContent.get("images"), wmNews);
            flag=true;
            if (!flag) {
                return;
            }

            //审核成功，修改文章状态
            ResponseResult responseResult = saveAppArticle(wmNews);
            if(!responseResult.getCode().equals(200)){
                throw new RuntimeException("WmNewsAutoScanServiceImpl - 保存文章失败");
            }
            //回填ArticleId
            wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews,(short)9,"审核成功");
        }

    }

    @Autowired
    private IArticleClient iArticleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关文章
     *
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        //属性拷贝
        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);
        //文章布局
        articleDto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            articleDto.setChannelName(wmChannel.getName());
        }
        //作者
        articleDto.setAuthorId(Long.valueOf(wmNews.getUserId()));
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            articleDto.setAuthorName(wmUser.getName());
        }

        //设置文章id，如果不为空，则说明是修改，还将之前的id赋值给他，后面的article服务还会进行数据更新
        if (wmNews.getArticleId() != null) {
            articleDto.setId(wmNews.getArticleId());
        }
        articleDto.setCreatedTime(new Date());

        ResponseResult responseResult = iArticleClient.saveArticle(articleDto);
        return responseResult;
    }

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GreenImageScan greenImageScan;

    /**
     * 图片审核
     *
     * @param images
     * @param wmNews
     * @return
     */
    private Boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;

        if (images == null || images.isEmpty()) {
            return flag;
        }

        //下载图片从minio中
        //图片去重
        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();
        for (String image : images) {
            byte[] bytes = fileStorageService.downLoadFile(image);
            imageList.add(bytes);
        }

        //进行审核
        try {
            Map map = greenImageScan.imageScan(imageList);
            if (map != null) {
                //审核失败
                if (map.get("suggestion").equals("block")) {
                    flag = false;
                    updateWmNews(wmNews, 2, "当前文章存在违规内容");
                }

                //不确定信息，人工审核
                if (map.get("suggestion").equals("review")) {
                    flag = false;
                    updateWmNews(wmNews, 3, "当前文章存在不确定内容");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;

    /**
     * 文章内容审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private Boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;

        try {
            Map map = greenTextScan.greeTextScan(content);
            if (map != null) {
                //审核失败
                if (map.get("suggestion").equals("block")) {
                    flag = false;
                    updateWmNews(wmNews, 2, "当前文章存在违规内容");
                }

                //不确定信息，人工审核
                if (map.get("suggestion").equals("review")) {
                    flag = false;
                    updateWmNews(wmNews, 3, "当前文章存在不确定内容");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }

    /**
     * 修改文章内容
     *
     * @param wmNews
     * @param x
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, int x, String reason) {
        wmNews.setStatus((short) x);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从文章中提取文章内容、图片（封面、内容图片）
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> extractImageAndContent(WmNews wmNews) {
        StringBuilder content = new StringBuilder();
        List<String> images = new ArrayList<>();

        //从自媒体文章中提取图片与文字
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if ("text".equals(map.get("type"))) {
                    content.append(map.get("value"));
                }

                if ("image".equals(map.get("type"))) {
                    images.add((String) map.get("value"));
                }
            }
        }
        //提取封面图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        //返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("content", content.toString());
        result.put("images", images);

        return result;
    }


}
