package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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
}
