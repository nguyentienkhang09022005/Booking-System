package com.example.api_gateway.filter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter
{
    private final WebClient.Builder webClientBuilder;

    @Bean
    public GlobalFilter globalFilter()
    {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (path.startsWith("/api/authen/login") ||
                    path.startsWith("/api/authen/register") ||
                    path.startsWith("/api/authen/refresh")) {
                return chain.filter(exchange);
        }

        // Lấy token từ header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Gọi sang Authen Service để introspect token
        return webClientBuilder.build()
                .post()
                .uri("http://localhost:8081/api/authen/introspect")
                .bodyValue("{\"token\":\"" + token + "\"}")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    // Nếu token valid thì cho qua
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
        };
    }
}
