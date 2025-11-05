package com.example.api_gateway.config;

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
    @Value("${authentication.service}")
    protected String AUTHENTICATION_SERVICE;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(AUTHENTICATION_SERVICE)
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create().followRedirect(true)
                        )
                )
                .build();
    }
}


