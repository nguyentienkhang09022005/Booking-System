package com.example.Booking_Hotel_BE.Services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenService
{
    final RedisTemplate<String, String> redisTemplate;

    static final String PREFIX = "refreshToken: ";

    public void saveRefreshToken(String idUser, String refreshToken, long ttlSeconds)
    {
        String key = PREFIX + idUser;
        redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getRefreshToken(String idUser) {
        return redisTemplate.opsForValue().get(PREFIX + idUser);
    }

    public void deleteRefreshToken(String idUser) {
        redisTemplate.delete(PREFIX + idUser);
    }
}
