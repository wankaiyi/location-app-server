package com.wky.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wky.dto.LocationIn;
import com.wky.entity.Location;
import com.wky.mapper.LocationMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LocationController {

    @Autowired
    private LocationMapper locationMapper;

    @PostMapping("/location")
    public Location postLocation(@Valid @RequestBody LocationIn in) {
        Location loc = new Location();
        loc.setDeviceId(in.getDeviceId());
        loc.setLat(in.getLat());
        loc.setLng(in.getLng());
        loc.setAccuracy(in.getAccuracy());
        loc.setProvider(in.getProvider());
        loc.setCreatedAt(LocalDateTime.now());
        locationMapper.insert(loc);
        return loc;
    }

    @GetMapping("/locations")
    public List<Location> list(@RequestParam(required = false) String device_id,
                               @RequestParam(defaultValue = "200") Integer limit) {
        QueryWrapper<Location> qw = new QueryWrapper<>();
        if (device_id != null && !device_id.isEmpty()) {
            qw.eq("device_id", device_id);
        }
        qw.orderByDesc("created_at").last("limit " + limit);
        return locationMapper.selectList(qw);
    }
}