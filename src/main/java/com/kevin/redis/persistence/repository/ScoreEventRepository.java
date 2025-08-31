package com.kevin.redis.persistence.repository;

import com.kevin.redis.persistence.model.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {

}

