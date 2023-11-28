package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService {

    /**
     * 敏感词查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(SensitiveDto dto) {

        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        page = page(page, Wrappers.<WmSensitive>lambdaQuery().eq(StringUtils.isNotBlank(dto.getName()), WmSensitive::getSensitives, dto.getName()));

        //返回数据
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    /**
     * 新增数据
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult add(WmSensitive wmSensitive) {

        //参数校验
        if (wmSensitive == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断是否重复
        int count = count(Wrappers.<WmSensitive>lambdaQuery()
                .eq(StringUtils.isNotBlank(wmSensitive.getSensitives()), WmSensitive::getSensitives, wmSensitive.getSensitives()));
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST);
        }

        //数据填充
        wmSensitive.setCreatedTime(new Date());

        //保存数据
        save(wmSensitive);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 修改数据
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult update(WmSensitive wmSensitive) {

        //参数校验
        if (wmSensitive == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断数据是否已经存在
        WmSensitive sensitiveDb = getById(wmSensitive.getId());
        if (!wmSensitive.getSensitives().equals(sensitiveDb.getSensitives())) {
            int count = count(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives, wmSensitive.getSensitives()));
            if(count>0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST);
            }
        }

        //修改数据
        updateById(wmSensitive);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除数据
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult del(Integer id) {

        //参数校验
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询数据
        WmSensitive sensitive = getById(id);
        if(sensitive==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //删除数据
        removeById(id);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
