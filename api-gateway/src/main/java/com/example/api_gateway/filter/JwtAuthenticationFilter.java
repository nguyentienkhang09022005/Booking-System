package com.example.api_gateway.filter;

import com.example.api_gateway.dto.request.IntrospectRequest;
import com.example.api_gateway.dto.response.IntrospectResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    final WebClient.Builder webClientBuilder; // dùng reactive WebClient để call AuthenService


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();
        // Các API public không yêu cầu Authorization
        if (path.contains("/api/authen/login")
                || path.contains("/api/authen/register")
                || path.contains("/api/authen/introspect")
                || path.contains("/api/google/login")) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        return webClientBuilder.build()
                .post()
                .uri("https://booking-hotel-be-spf2.onrender.com/api/authen/introspect") // gọi sang Authen Service
                .bodyValue(new IntrospectRequest(token))
                .retrieve()
                .bodyToMono(IntrospectResponse.class)
                .flatMap(response -> {
                    if (!response.getValid()) {
                        return onError(exchange, "Invalid JWT Token", HttpStatus.UNAUTHORIZED);
                    }

                    // Gắn thêm userId, role vào header cho các service khác
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                            .header("X-User-Id", response.getUserId())
//                            .header("X-Role", response.getRole())
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(e -> onError(exchange, "Authen Service Error", HttpStatus.UNAUTHORIZED));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status)
    {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}


