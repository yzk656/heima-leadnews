package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    /**
     * 查询全部
     *
     * @return
     */
    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }


    /**
     * 保存频道
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult saveChannel(WmChannel wmChannel) {
        //参数校验
        if (wmChannel.getName() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //频道名称不能相同
        /*LambdaQueryWrapper<WmChannel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmChannel::getName, wmChannel.getName());
        WmChannel channel = getOne(queryWrapper);*/
        WmChannel channel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, wmChannel.getName()));
        if (channel != null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "该频道已经存在");
        }

        //数据填充
        wmChannel.setCreatedTime(new Date());
        wmChannel.setIsDefault(true);

        //保存
        save(wmChannel);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 列表查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(ChannelDto dto) {
        //检验参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查分页
        dto.checkParam();

        //条件分页查询
        IPage page = new Page<>(dto.getPage(), dto.getSize());
        page = page(page, Wrappers.<WmChannel>lambdaQuery()
                .like(StringUtils.isNotBlank(dto.getName()), WmChannel::getName, dto.getName())
                .orderByDesc(WmChannel::getCreatedTime)
        );

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsService wmNewsService;

    /**
     * 修改频道
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult update(WmChannel wmChannel) {
        //参数校验
        if (wmChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断数据是否已经被使用
        int wmNewsCount = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, wmChannel.getId())
                .eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode())
        );
        if (wmNewsCount > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道已经被引用，不能修改");
        }

        //频道名称不能与其他名称重复，除了当前频道
        WmChannel tempChannel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, wmChannel.getName()));
        if (tempChannel != null &&
                !wmChannel.getId().equals(tempChannel.getId()) &&//频道名称可以不变
                wmChannel.getName().equals(tempChannel.getName())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "该频道名称已经被使用，修改失败");
        }

        //更新数据
        updateById(wmChannel);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    /**
     * 删除频道
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult del(Integer id) {
        //参数校验
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel wmChannel = getById(id);
        if (wmChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //检查频道是否有效
        if (wmChannel.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道有效，不能删除");
        }

        //检查当前频道是否还在被引用
        int count = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, id)
                .eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode())
        );
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道被引用，不能删除");
        }

        //进行删除
        removeById(id);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
