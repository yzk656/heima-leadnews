package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.sun.org.apache.regexp.internal.RE;
import io.swagger.models.auth.In;

public interface WmSensitiveService extends IService<WmSensitive> {

    /**
     * 敏感词查询
     * @param dto
     * @return
     */
    ResponseResult findList(SensitiveDto dto);

    /**
     * 新增数据
     * @param wmSensitive
     * @return
     */
    ResponseResult add(WmSensitive wmSensitive);

    /**
     * 修改数据
     * @param wmSensitive
     * @return
     */
    ResponseResult update(WmSensitive wmSensitive);

    /**
     * 删除数据
     * @param id
     * @return
     */
    ResponseResult del(Integer id);
}
