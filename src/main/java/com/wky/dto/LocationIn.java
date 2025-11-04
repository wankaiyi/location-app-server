package com.wky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationIn {
    @NotBlank
    private String deviceId;
    @NotNull
    private Double lat;
    @NotNull
    private Double lng;
    private Double accuracy;
    private String provider;
}