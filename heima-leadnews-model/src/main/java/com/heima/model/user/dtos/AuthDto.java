package com.heima.model.user.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class AuthDto extends PageRequestDto {
    private Integer id;
    private String msg;
    private Integer status;
}
