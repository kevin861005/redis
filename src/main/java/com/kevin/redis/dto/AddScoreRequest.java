package com.kevin.redis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddScoreRequest {

    @NotBlank
    private String member;

    @NotNull
    private Double delta; // 可正可負

}
