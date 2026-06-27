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

    private String licensePlatePath = "/v1/ocr/license-plate";

    /**
     * Bật/tắt các bước đối chiếu nội dung giấy tờ (họ tên CCCD khớp bằng lái, biển số khớp giấy
     * đăng ký xe, ảnh không bị mờ/giả). Mặc định BẬT (true) cho môi trường thật. Tạm để false khi
     * cần test nhanh luồng đăng ký mà chưa có ảnh giấy tờ khớp nhau hoàn chỉnh — nhớ trả lại true
     * trước khi demo/nghiệm thu hoặc deploy.
     */
    private boolean validationEnabled = true;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
