package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;

public interface ApUserSearchService {

    /**
     * 保存用户搜索记录
     * @param keyword
     * @param userId
     */
    void insert(String keyword, Integer userId);

    /**
     * 加载用户搜索记录
     * @return
     */
    ResponseResult loadHistory();

    /**
     * 删除用户搜索记录
     * @param dto
     * @return
     */
    ResponseResult delHistory(HistorySearchDto dto);
}
