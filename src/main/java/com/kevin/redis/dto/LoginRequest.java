package com.kevin.redis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "帳號不可空白")
    private String username;

    @NotBlank(message = "密碼不可空白")
    private String password;
}
