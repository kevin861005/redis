package com.kevin.redis.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Rank {

    private String member;

    private double score;
}
