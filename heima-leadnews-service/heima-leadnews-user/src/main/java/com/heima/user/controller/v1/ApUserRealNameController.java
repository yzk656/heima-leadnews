package com.heima.user.controller.v1;

import com.heima.model.common.dtos.PageRequestDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.ApUserRealnameDto;
import com.heima.user.service.ApUserRealNameService;
import com.heima.user.service.ApUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class ApUserRealNameController {

    @Autowired
    private ApUserRealNameService apUserRealNameService;

    @PostMapping("/list")
    public ResponseResult list(@RequestBody ApUserRealnameDto dto) {
        return apUserRealNameService.list(dto);
    }
}
