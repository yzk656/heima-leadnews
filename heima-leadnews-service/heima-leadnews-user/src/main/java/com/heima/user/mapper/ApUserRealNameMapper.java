package com.heima.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.user.service.ApUserRealNameService;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApUserRealNameMapper extends BaseMapper<ApUserRealname> {
}
