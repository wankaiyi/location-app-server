package com.wky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wky.entity.Location;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {
}