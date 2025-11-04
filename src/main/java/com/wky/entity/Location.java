package com.wky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("locations")
public class Location {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private Double lat;
    private Double lng;
    private Double accuracy;
    private String provider;
    private LocalDateTime createdAt;
}