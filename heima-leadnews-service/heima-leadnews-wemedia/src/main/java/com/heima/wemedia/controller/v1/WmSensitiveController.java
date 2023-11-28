package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensitive")
public class WmSensitiveController {

    @Autowired
    private WmSensitiveService wmSensitiveService;

    @PostMapping("/list")
    public ResponseResult list(@RequestBody SensitiveDto dto) {
        return wmSensitiveService.findList(dto);
    }

    @PostMapping("/save")
    public ResponseResult save(@RequestBody WmSensitive wmSensitive){
        return wmSensitiveService.add(wmSensitive);
    }

    @PostMapping("/update")
    public ResponseResult update(@RequestBody WmSensitive wmSensitive){
        return wmSensitiveService.update(wmSensitive);
    }

    @DeleteMapping("/del/{id}")
    public ResponseResult del(@PathVariable("id") Integer id){
        return wmSensitiveService.del(id);
    }
}
