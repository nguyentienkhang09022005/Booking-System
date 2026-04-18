package com.example.java_service.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RagCacheService
{
    RedisTemplate<String, String> redisTemplate;
    ObjectMapper objectMapper;

    static final String PREFIX = "rag_cache:";
    private static final long DEFAULT_TTL_SECONDS = 300;

    private String getKey(String cacheType, String cacheKey) {
        return PREFIX + cacheType + ":" + cacheKey.toLowerCase().trim();
    }

    public <T> T get(String cacheType, String cacheKey, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(getKey(cacheType, cacheKey));
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Cache get failed for {}/{}: {}", cacheType, cacheKey, e.getMessage());
            return null;
        }
    }

    public <T> T getList(String cacheType, String cacheKey, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(getKey(cacheType, cacheKey));
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("Cache get failed for {}/{}: {}", cacheType, cacheKey, e.getMessage());
            return null;
        }
    }

    public void set(String cacheType, String cacheKey, Object value) {
        set(cacheType, cacheKey, value, DEFAULT_TTL_SECONDS);
    }

    public void set(String cacheType, String cacheKey, Object value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(getKey(cacheType, cacheKey), json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Cached {}/{} with TTL {}s", cacheType, cacheKey, ttlSeconds);
        } catch (Exception e) {
            log.warn("Cache set failed for {}/{}: {}", cacheType, cacheKey, e.getMessage());
        }
    }

    public void evict(String cacheType, String cacheKey) {
        try {
            redisTemplate.delete(getKey(cacheType, cacheKey));
            log.debug("Evicted cache {}/{}", cacheType, cacheKey);
        } catch (Exception e) {
            log.warn("Cache evict failed for {}/{}: {}", cacheType, cacheKey, e.getMessage());
        }
    }

    public void evictPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cache entries matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Cache evict pattern failed for {}: {}", pattern, e.getMessage());
        }
    }

    public void evictAccommodationCache() {
        evictPattern("accommodation:*");
    }

    public void evictTourCache() {
        evictPattern("tour:*");
    }

    public void evictVoucherCache() {
        evictPattern("voucher:*");
    }

    public void evictAllRagCache() {
        evictPattern("*");
    }
}