package com.example.Booking_Hotel_BE.Config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(maxAge)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                            .path("/")
                            .httpOnly(true)
                            .secure(true)
                            .maxAge(0)
                            .sameSite("None")
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
                }
            }
        }
    }

    public static String serialize(Serializable object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            return cls.cast(SerializationUtils.deserialize(
                    Base64.getUrlDecoder().decode(cookie.getValue())));
        } catch (Exception e) {
            return null;
        }
    }
}