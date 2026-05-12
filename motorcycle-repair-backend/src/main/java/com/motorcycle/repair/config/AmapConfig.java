package com.motorcycle.repair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    private String apiKey;
    private String baseUrl = "https://restapi.amap.com/v3";
}
