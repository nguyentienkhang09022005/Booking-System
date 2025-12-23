package com.example.Booking_Hotel_BE.Config;

import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Services.AuthenService;
import com.example.Booking_Hotel_BE.Services.CustomOAuth2UserService;
import com.example.Booking_Hotel_BE.Services.RefreshTokenService;
import com.example.Booking_Hotel_BE.Services.RegisterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig
{
    private final CustomJwtDecoder customJwtDecoder;
    private final RegisterService registerService;
    private final AuthenService authenService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;


    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected Long REFRESHABLE_DURATION;

    @Value("${app.frontend.url}")
    private String FRONTEND_URL;

    public SecurityConfig(CustomJwtDecoder customJwtDecoder,
                          @Lazy RegisterService registerService,
                          @Lazy AuthenService authenService,
                          @Lazy RefreshTokenService refreshTokenService,
                          @Lazy UserMapper userMapper)
    {
        this.customJwtDecoder = customJwtDecoder;
        this.registerService = registerService;
        this.authenService = authenService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;

    }
    // Tách riêng các endpoint của Swagger để dễ quản lý
    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui.html"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/health",
            "/api/authen/**",
            "/api/register",
            "/api/otp/**",
            "/oauth2/authorization/google",
    };

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(registerService, authenService, refreshTokenService, userMapper);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity

                // Stateless session (không dùng session để lưu auth)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Cấu hình các Endpoint
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll() // Đảm bảo Swagger được cho phép
                        .anyRequest().authenticated()
                )
                // Cấu hình đăng nhập tạo TOKEN
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                // Cấu hình đăng nhập bằng bên thứ 3(Google)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                        )
                        // Cấu hình redirectionEndpoint
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService())
                        )
                        .successHandler((request, response, authentication) -> {
                            CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
                            AuthenResponse authResponse = oauthUser.getAuthResponse();

                            // Tạo Cookie lưu refreshToken
                            String cookieHeader = String.format(
                                    "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                                    authResponse.getRefreshToken(),
                                    REFRESHABLE_DURATION
                            );
                            response.addHeader("Set-Cookie", cookieHeader);

                            String infUserEncoded = "";
                            try {
                                String infUserJson = objectMapper.writeValueAsString(authResponse.getInfUser());

                                infUserEncoded = Base64.getUrlEncoder()
                                        .encodeToString(infUserJson.getBytes(StandardCharsets.UTF_8));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                            String targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_URL + "/oauth2/redirect")
                                    .queryParam("token", authResponse.getToken())
                                    .queryParam("user", infUserEncoded)
                                    .build().toUriString();

                            response.sendRedirect(targetUrl);
                        })
                )
                .csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

}
