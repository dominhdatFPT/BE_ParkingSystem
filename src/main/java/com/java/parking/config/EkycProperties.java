package com.swp.parking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConfigurationProperties(prefix = "ekyc")
@Data
public class EkycProperties {

    private String provider = "vnpt";

    private String baseUrl;

    private String token;

    private String tokenId;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
