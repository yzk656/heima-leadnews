package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import io.swagger.models.auth.In;

public interface WmChannelService extends IService<WmChannel> {
    /**
     * 查询全部
     * @return
     */
    public ResponseResult findAll();

    /**
     * 保存频道
     * @param wmChannel
     * @return
     */
    ResponseResult saveChannel(WmChannel wmChannel);

    /**
     * 列表查询
     * @param dto
     * @return
     */
    ResponseResult list(ChannelDto dto);

    /**
     * 修改频道
     * @param wmChannel
     * @return
     */
    ResponseResult update(WmChannel wmChannel);

    /**
     * 删除频道
     * @param id
     * @return
     */
    ResponseResult del(Integer id);
}
