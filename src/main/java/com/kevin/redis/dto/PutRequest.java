package com.kevin.redis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutRequest {

    @NotBlank
    private String key;

    @NotBlank
    private String value;

    @NotNull
    private Long ttlSeconds;

}
