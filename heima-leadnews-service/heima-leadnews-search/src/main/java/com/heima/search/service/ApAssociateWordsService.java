package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.pojos.ApUserSearch;

public interface ApAssociateWordsService {

    /**
     * 联想词搜索
     * @param dto
     * @return
     */
    ResponseResult search(UserSearchDto dto);
}
