package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.ApUserRealNameConstants;
import com.heima.model.common.dtos.PageRequestDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.ApUserRealnameDto;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.user.mapper.ApUserRealNameMapper;
import com.heima.user.service.ApUserRealNameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ApUserRealNameServiceImpl extends ServiceImpl<ApUserRealNameMapper, ApUserRealname> implements ApUserRealNameService {
    /**
     * 用户审核查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(ApUserRealnameDto dto) {

        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        page = page(page, Wrappers.<ApUserRealname>lambdaQuery()
                .eq(dto.getStatus()!=null,ApUserRealname::getStatus,dto.getStatus())
                .orderByDesc(ApUserRealname::getCreatedTime));

        //赋值返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }


    /**
     * 通过审核
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult pass(AuthDto dto) {

        //参数校验
        if (dto==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取当前用户
        ApUserRealname user = getById(dto.getId());
        update(Wrappers.<ApUserRealname>lambdaUpdate()
                .set(user.getStatus()==1,ApUserRealname::getStatus, ApUserRealNameConstants.AP_USER_REAL_NAME_PASS)
                .eq(ApUserRealname::getId,dto.getId())
        );

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 审核失败
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult authFail(AuthDto dto) {

        //参数校验
        if(dto==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取当前用户
        ApUserRealname user = getById(dto.getId());
        update(Wrappers.<ApUserRealname>lambdaUpdate()
                .set(user.getStatus()==1,ApUserRealname::getStatus,ApUserRealNameConstants.AP_USER_REAL_NAME_FAIL)
                .set(dto.getMsg()!=null,ApUserRealname::getReason,dto.getMsg())
                .eq(ApUserRealname::getId,dto.getId())
        );

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
