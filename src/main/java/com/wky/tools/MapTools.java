package com.wky.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.wky.client.GaodeMapClient;
import com.wky.utils.CoordinateTransformUtils;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author wky
 * @date 2025/11/06
 */
@Component
@RequiredArgsConstructor
public class MapTools {

    private final GaodeMapClient gaodeMapClient;

    private final LoadingCache<String, String> locationCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES) // 写入后10分钟过期
            .expireAfterAccess(5, TimeUnit.MINUTES) // 访问后5分钟过期
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public String load(@NotNull String key) {
                    // 当缓存不存在时，自动加载数据
                    String[] lngLat = key.split(",");
                    return loadAddress(Double.parseDouble(lngLat[0]), Double.parseDouble(lngLat[1]));
                }
            });

    public String loadAddress(double lng, double lat) {
        JsonNode location = gaodeMapClient.getLocation(lng + "," + lat);
        if (location == null) {
            return "未查询到地址";
        }
        return location.get("regeocode").get("formatted_address").asText();
    }

    @Tool(name = "get-address-by-lng-lat", description = "根据经纬度查询地址")
    String getAddress(@ToolParam(description = "经度") double lng, @ToolParam(description = "纬度") double lat) throws ExecutionException {
        double[] gcj02LatLng = CoordinateTransformUtils.wgs84ToGcj02(lat, lng);
        return locationCache.get(gcj02LatLng[1] + "," + gcj02LatLng[0]);
    }

}
