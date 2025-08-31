package com.kevin.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class Rank {

    private String member;

    private double score;
}
