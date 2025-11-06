package com.wky.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author wky
 * @date 2025/11/06
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GaodeMapClient {

    private final RestTemplate restTemplate;

    private static final String GET_LOCATION_URL = "https://restapi.amap.com/v3/geocode/regeo";

    @Value("${gaode.key}")
    private String key;

    public JsonNode getLocation(String location) {
        try {
            log.info("Getting location from GaodeMap API for {}", location);
            String url = GET_LOCATION_URL + "?key=" + key + "&location=" + location + "&extensions=base";
            ResponseEntity<JsonNode> loc = restTemplate.getForEntity(url, JsonNode.class);
            log.info("Got location from GaodeMap API response: {}", loc.getBody());

            JsonNode result = loc.getBody();
            if ("1".equals(result.get("status").asText())) {
                return result;
            } else {
                return null;
            }
        } catch (RestClientException e) {
            log.error("Failed to get location from GaodeMap API", e);
            return null;
        }
    }

}
