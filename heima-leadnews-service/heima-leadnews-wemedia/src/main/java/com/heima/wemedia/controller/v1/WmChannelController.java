package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
public class WmChannelController {

    @Autowired
    private WmChannelService wmChannelService;

    @GetMapping("/channels")
    public ResponseResult findAll() {
        return wmChannelService.findAll();
    }

    @PostMapping("/save")
    public ResponseResult save(@RequestBody WmChannel wmChannel) {
        return wmChannelService.saveChannel(wmChannel);
    }

    @PostMapping("/list")
    public ResponseResult list(@RequestBody ChannelDto dto) {
        return wmChannelService.list(dto);
    }

    @PostMapping("/update")
    public ResponseResult update(@RequestBody WmChannel wmChannel) {
        return wmChannelService.update(wmChannel);
    }

    @GetMapping("/del/{id}")
    public ResponseResult del(@PathVariable("id") Integer id){
        return wmChannelService.del(id);
    }
}
