package com.kevin.redis.persistence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table (name = "score_events")
@Getter
@Setter
public class ScoreEvent {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false)
    private double delta;

    @Column(length=100)
    private String reason;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public ScoreEvent() {}

    public ScoreEvent(Long userId, double delta, String reason) {
        this.userId = userId; this.delta = delta; this.reason = reason;
    }
}
