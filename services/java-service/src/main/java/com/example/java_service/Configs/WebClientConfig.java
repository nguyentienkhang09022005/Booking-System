package com.example.java_service.Configs;

import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {
    @NonFinal
    @Value("${base.url}")
    protected String GOLANG_SERVICE_URL;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(GOLANG_SERVICE_URL)
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().followRedirect(true)
                        )
                )
                .build();
    }
}

