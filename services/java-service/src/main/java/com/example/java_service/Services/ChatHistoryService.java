package com.example.java_service.Services;

import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatHistoryService
{
    final RedisTemplate<String, String> redisTemplate;

    static final String PREFIX = "chat_history:";
    ObjectMapper objectMapper;

    private static final int MAX_HISTORY_MESSAGES = 15;
    private static final long DEFAULT_TTL_SECONDS = 604800L;

    private String getKey(String idUser) {
        return PREFIX + idUser;
    }

    public void saveChat(String idUser, MessageHistoryItem messageHistoryItem, long ttlSeconds)
    {
        try {
            String existing = redisTemplate.opsForValue().get(getKey(idUser));

            List<MessageHistoryItem> history;
            if (existing != null && !existing.isEmpty()) {
                history = objectMapper.readValue(existing, new TypeReference<List<MessageHistoryItem>>() {});
            } else {
                history = new ArrayList<>();
            }

            history.add(messageHistoryItem);

            if (history.size() > MAX_HISTORY_MESSAGES) {
                int excess = history.size() - MAX_HISTORY_MESSAGES;
                history = history.subList(excess, history.size());
                log.info("Truncated chat history for user {}, removed {} old messages", idUser, excess);
            }

            String json = objectMapper.writeValueAsString(history);

            redisTemplate.opsForValue().set(getKey(idUser), json, ttlSeconds, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử chat", e);
        }
    }

    public List<MessageHistoryItem> getHistoryChat(String idUser)
    {
        try {
            String json = redisTemplate.opsForValue().get(getKey(idUser));

            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }

            List<MessageHistoryItem> history = objectMapper.readValue(json, new TypeReference<List<MessageHistoryItem>>() {});

            if (history.size() > MAX_HISTORY_MESSAGES) {
                return history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size());
            }

            return history;
        }
        catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử chat", e);
            return new ArrayList<>();
        }
    }

    public void deleteHistoryChat(String idUser) {
        redisTemplate.delete(getKey(idUser));
    }
}