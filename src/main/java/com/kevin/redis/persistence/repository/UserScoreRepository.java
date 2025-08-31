package com.kevin.redis.persistence.repository;

import com.kevin.redis.persistence.model.UserScore;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserScoreRepository extends JpaRepository<UserScore, Long> {
    // 將分數累加到 user_scores（UPSERT）
    @Modifying
    @Transactional
    @Query (value = """
    INSERT INTO user_scores(user_id, score) VALUES (:userId, :delta)
    ON CONFLICT (user_id) DO UPDATE SET score = user_scores.score + EXCLUDED.score
    """, nativeQuery = true)
    int upsertAndAdd(@Param ("userId") Long userId, @Param("delta") double delta);

    // 取最新分數
    @Query(value = "SELECT score FROM user_scores WHERE user_id = :userId", nativeQuery = true)
    Double findScore(@Param("userId") Long userId);

    // 取 DB TopN （供對帳）
    interface UsernameScore {
        String getUsername(); Double getScore();
    }

    @Query(value = """
    SELECT u.username AS username, s.score AS score
    FROM user_scores s JOIN users u ON u.id = s.user_id
    ORDER BY s.score DESC
    LIMIT :n
    """, nativeQuery = true)
    List<UsernameScore> topN(@Param("n") int n);
}
