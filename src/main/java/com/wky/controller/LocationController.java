package com.wky.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wky.dto.LocationIn;
import com.wky.entity.Location;
import com.wky.mapper.LocationMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class LocationController {

    @Autowired
    private LocationMapper locationMapper;

    private static final Cache<String, Boolean> requestCache= CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES) // 1分钟后自动过期
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build();


    /**
     * 检查是否在同一分钟内已经处理过相同标识的请求
     * @return true-可以处理(未重复), false-重复请求
     */
    public boolean shouldProcess(LocalDateTime time) {
        String minute = getMinute(time);

        // 如果key不存在，则放入缓存并返回true；如果已存在，返回false
        Boolean result = requestCache.asMap().putIfAbsent(minute, true);
        return result == null;
    }

    private String getMinute(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    @PostMapping("/location")
    public ResponseEntity<Void> postLocation(@Valid @RequestBody LocationIn in) {
        LocalDateTime time = in.getTime().truncatedTo(ChronoUnit.MINUTES);
        if (!shouldProcess(time)) {
            return ResponseEntity.noContent().build();
        }
        Location loc = new Location();
        loc.setDeviceId(in.getDeviceId());
        loc.setLat(in.getLat());
        loc.setLng(in.getLng());
        loc.setAccuracy(in.getAccuracy());
        loc.setProvider(in.getProvider());
        loc.setCreatedAt(time);
        locationMapper.insert(loc);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/locations")
    public List<Location> list(@RequestParam(required = false) String device_id,
                               @RequestParam(defaultValue = "200") Integer limit,
                               LocalDateTime time) {
        QueryWrapper<Location> qw = new QueryWrapper<>();
        if (device_id != null && !device_id.isEmpty()) {
            qw.eq("device_id", device_id);
        }
        if (time != null) {
            qw.in("created_at", time);
        }
        qw.orderByDesc("created_at").last("limit " + limit);
        return locationMapper.selectList(qw);
    }
}