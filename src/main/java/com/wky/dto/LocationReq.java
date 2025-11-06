package com.wky.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wky
 * @date 2025/11/06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationReq {

    private String location;
    private String poitype;
    private Integer radius;
    // base、all
    private String extensions;
    // 0、1
    private Integer roadlevel;
}
