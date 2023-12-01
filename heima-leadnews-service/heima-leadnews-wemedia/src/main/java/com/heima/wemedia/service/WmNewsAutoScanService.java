package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsAutoScanService extends IService<WmNews> {

    /**
     * 审核文章
     * @param id
     */
    public void autoScanWmNews(Integer id);

    public ResponseResult saveAppArticle(WmNews wmNews);
}
