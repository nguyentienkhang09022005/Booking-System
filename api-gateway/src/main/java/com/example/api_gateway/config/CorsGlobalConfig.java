package com.example.api_gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsGlobalConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Áp dụng cho tất cả các đường dẫn (path)
                .allowedOriginPatterns("*") // Cho phép tất cả các nguồn gốc (origin)
                .allowedMethods("*") // Cho phép tất cả các phương thức HTTP (GET, POST, etc.)
                .allowedHeaders("*") // Cho phép tất cả các header
                .allowCredentials(true); // Cho phép gửi cookie và thông tin xác thực
    }
}
