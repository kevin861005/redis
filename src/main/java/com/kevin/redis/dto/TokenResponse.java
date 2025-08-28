package com.kevin.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {

    private String token;

    private long expiresInSeconds;
}
