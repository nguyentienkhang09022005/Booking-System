package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Config.CustomOAuth2User;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Dto.Response.UserResponse;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Models.Users;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomOAuth2UserService extends OidcUserService
{
    RegisterService registerService;
    AuthenService authenService;
    RefreshTokenService refreshTokenService;
    UserMapper userMapper;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected Long REFRESHABLE_DURATION;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String urlAvatar = (String) attributes.get("picture");

        if (email == null) {
            throw new OAuth2AuthenticationException("Google account does not provide email");
        }

        String firstName = "";
        String lastName = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            }
        }
        Users user = registerService.processOAuth2User(email, firstName, lastName, urlAvatar);

        if (user.getIdUser() == null) {
            throw new OAuth2AuthenticationException("Lưu user thất bại");
        }

        String token = authenService.generateAccessToken(user);
        String refreshToken = authenService.generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(
                user.getIdUser().toString(),
                refreshToken,
                REFRESHABLE_DURATION
        );

        UserResponse userResponse = userMapper.toUserResponse(user);

        AuthenResponse authResponse = AuthenResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .infUser(userResponse)
                .build();

        return new CustomOAuth2User(oidcUser, authResponse);
    }
}