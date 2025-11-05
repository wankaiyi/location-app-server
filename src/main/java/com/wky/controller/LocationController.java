package com.wky.controller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wky.dto.LocationIn;
import com.wky.entity.Location;
import com.wky.mapper.LocationMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@Slf4j
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
        String traceId = UUID.randomUUID().toString();
        log.info("request: {}, traceId: {}", in, traceId);
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
        loc.setDeleted(0);
        locationMapper.insert(loc);
        return ResponseEntity.ok().build();
    }

    // 新增：查询所有设备ID（去重）
    @GetMapping("/devices")
    public List<String> listDevices() {
        return locationMapper.selectDistinctDeviceIds();
    }

    // 新增：按天查询某设备的轨迹
    @GetMapping("/device-day")
    public List<Location> listDeviceDay(@RequestParam("deviceId") String deviceId,
                                        @RequestParam("date") String date) {
        LocalDate day = LocalDate.parse(date); // 格式: YYYY-MM-DD
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        return locationMapper.selectByDeviceAndDate(deviceId, start, end);
    }
}