package com.kevin.redis.controller;

import com.kevin.redis.dto.LoginRequest;
import com.kevin.redis.dto.TokenResponse;
import com.kevin.redis.dto.UserInfo;
import com.kevin.redis.service.AuthService;
import com.kevin.redis.utils.TokenUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 登入 / 取得自身資訊 / 登出
 * - /login  假裝驗證成功 -> 存 Redis
 * - /me     讀 Redis
 * - /logout 刪 Redis
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        // 假裝帳密驗證成功（實務上請接 DB / LDAP / OAuth2）
        String token = TokenUtils.generateToken();

        UserInfo user = UserInfo.builder()
                                .username(req.getUsername())
                                .displayName("Hi, " + req.getUsername())
                                .role("USER")
                                .build();

        authService.saveSession(token, user);
        log.info("登入成功, username={}, token={}", req.getUsername(), token);

        return ResponseEntity.ok(new TokenResponse(token, authService.getTtlSeconds()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        String token = extractBearer(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body("缺少或無效的 Authorization: Bearer <token>");
        }

        Optional<UserInfo> user = authService.getUserByToken(token);
        return user.<ResponseEntity<?>> map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.status(401).body("Token 已失效或不存在"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        String token = extractBearer(authHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body("缺少 Authorization: Bearer <token>");
        }
        authService.logout(token);
        return ResponseEntity.ok("已登出");
    }

    private String extractBearer(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        String prefix = "Bearer ";
        return authHeader.startsWith(prefix) ? authHeader.substring(prefix.length()) : null;
    }
}
