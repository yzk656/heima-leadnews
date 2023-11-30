package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.PageRequestDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.ApUserRealnameDto;
import com.heima.model.user.pojos.ApUserRealname;

public interface ApUserRealNameService extends IService<ApUserRealname> {

    /**
     * 用户审核查询
     * @param dto
     * @return
     */
    ResponseResult list(ApUserRealnameDto dto);
}
