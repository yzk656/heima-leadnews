package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.model.admin.dtos.AdUserDto;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.HashMap;

@Service
@Transactional
@Slf4j
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {

    @Autowired
    private AdUserMapper adUserMapper;


    /**
     * 用户登陆
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(AdUserDto dto) {
        //参数校验
        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询是否包含当前用户
        LambdaQueryWrapper<AdUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdUser::getName, dto.getName());
        AdUser user = getOne(queryWrapper);
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.AD_USER_DATA_NOT_EXIST);
        }

        //判断密码是否正确
        String salt = user.getSalt();
        String password= dto.getPassword();
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        if(!md5DigestAsHex.equals(user.getPassword())){
            return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }

        //返回token
        String token = AppJwtUtil.getToken(user.getId().longValue());
        user.setPassword("");
        user.setSalt("");
        HashMap<String, Object> map = new HashMap<>();
        map.put("user",user);
        map.put("token",token);

        return ResponseResult.okResult(map);
    }
}
