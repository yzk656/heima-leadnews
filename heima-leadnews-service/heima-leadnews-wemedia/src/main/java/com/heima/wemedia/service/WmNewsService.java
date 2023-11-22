package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 根据条件查询列表
     * @param dto
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto dto);


    /**
     * 修改发布文章或者保存为草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);


    /**
     * 文章上下架
     * @param wmNewsDto
     * @return
     */
    ResponseResult downOrUp(WmNewsDto wmNewsDto);
}
