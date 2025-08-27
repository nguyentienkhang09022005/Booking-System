package com.example.Booking_Hotel_BE.Config;

import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OidcUser
{
    private final OidcUser oidcUser;
    private final AuthenResponse authResponse;

    public CustomOAuth2User(OidcUser oidcUser, AuthenResponse authResponse) {
        this.oidcUser = oidcUser;
        this.authResponse = authResponse;
    }

    public AuthenResponse getAuthResponse() {
        return authResponse;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oidcUser.getAuthorities();
    }

    @Override
    public String getName() {
        return oidcUser.getName();
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }
}