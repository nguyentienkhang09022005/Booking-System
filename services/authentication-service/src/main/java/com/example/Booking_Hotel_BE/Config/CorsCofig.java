package com.example.Booking_Hotel_BE.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsCofig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // Cho phép tất cả các cổng của FE
                .allowedMethods("")  // Cấu hình các phương thức HTTP cho phép
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
