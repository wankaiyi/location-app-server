package com.wky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wky.entity.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {
    // 返回所有设备的唯一ID列表
    @Select("select distinct device_id from locations where is_deleted = 0 order by device_id")
    List<String> selectDistinctDeviceIds();

    // 返回某设备在指定日期范围内的所有轨迹（按时间升序）
    @Select("select * from locations where device_id = #{deviceId} and is_deleted = 0 and created_at >= #{start} and created_at < #{end} and provider not in ('unknown') order by created_at asc")
    List<Location> selectByDeviceAndDate(@Param("deviceId") String deviceId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}