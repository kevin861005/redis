package com.kevin.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevin.redis.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.session.ttl-minutes:30}")
    private long ttlMinutes;

    private static final String SESSION_PREFIX = "session:token:";

    private String keyOf(String token) {
        return SESSION_PREFIX + token;
    }

    /** 寫入 Session（JSON），設定 TTL */
    public void saveSession(String token, UserInfo info) {
        try {
            String json = mapper.writeValueAsString(info);
            redis.opsForValue().set(keyOf(token), json, Duration.ofMinutes(ttlMinutes));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化使用者資訊失敗", e);
        }
    }

    /** 依 Token 讀取使用者資訊（自動延長 TTL 可自行實作） */
    public Optional<UserInfo> getUserByToken(String token) {
        String json = redis.opsForValue().get(keyOf(token));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(json, UserInfo.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** 登出：刪除 Token */
    public void logout(String token) {
        redis.delete(keyOf(token));
    }

    public long getTtlSeconds() {
        return Duration.ofMinutes(ttlMinutes).toSeconds();
    }

}
