package com.example.api_gateway.filter;

import com.example.api_gateway.dto.request.IntrospectRequest;
import com.example.api_gateway.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    final WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();
        // Các API public không yêu cầu Authorization
        if (path.contains("/api/authen/login")
                || path.contains("/api/authen/register")
                || path.contains("/api/authen/introspect")
                || path.contains("/login/oauth2/code/google")
                || path.contains("/api/otp/")
                || path.contains("/api/authen/refresh")
                || path.contains("/api/authen/logout")
                || path.contains("/hotel/getListHotel")
                || path.contains("/room/getListRoom")
                || path.contains("/room/getInfRoom")
                || path.contains("/service/getListService")
                || path.contains("/hotel/getInfHotel")){
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "Thiếu header Authorization", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Header Authorization không hợp lệ", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        return webClientBuilder.build()
                .post()
                .uri("https://booking-hotel-be-spf2.onrender.com/api/authen/introspect") // gọi sang Authen Service
                .bodyValue(new IntrospectRequest(token))
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .flatMap(response -> {
                    var introspectData = response.getData();

                    if (introspectData == null || introspectData.getValid() == null || !introspectData.getValid()) {
                        return onError(exchange, "JWT Token không hợp lệ", HttpStatus.UNAUTHORIZED);
                    }

                    UUID idUser = introspectData.getIdUser();
                    String role = introspectData.getRole();

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("idUser", idUser.toString())
                            .header("Role", role)
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Gateway không kết nối được tới Authen Service. Nguyên nhân gốc: ", e);
                    return onError(exchange, "Lỗi khi kết nối tới dịch vụ xác thực", HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
