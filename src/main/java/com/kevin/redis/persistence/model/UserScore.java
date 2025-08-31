package com.kevin.redis.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table (name = "user_scores")
@Getter
@Setter
public class UserScore {
    @Id
    private Long userId;

    @Column (nullable=false)
    private double score = 0d;
}
