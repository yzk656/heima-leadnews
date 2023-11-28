package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class ChannelDto extends PageRequestDto {

    /**
     * 频道名称
     */
    private String name;

    /**
     * 当前页
     */
    private Integer page;

    /**
     * 每页显示数
     */
    private Integer size;
}
