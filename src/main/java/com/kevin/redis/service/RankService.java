package com.kevin.redis.service;

import com.kevin.redis.dto.Rank;
import com.kevin.redis.persistence.model.ScoreEvent;
import com.kevin.redis.persistence.model.User;
import com.kevin.redis.persistence.repository.ScoreEventRepository;
import com.kevin.redis.persistence.repository.UserRepository;
import com.kevin.redis.persistence.repository.UserScoreRepository;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final StringRedisTemplate redis;
    private final UserRepository userRepo;
    private final ScoreEventRepository eventRepo;
    private final UserScoreRepository userScoreRepo;

    @Value("${app.rank.key:rank:global}")
    private String rankKey;

    private static void ensureFinite(double v) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException("delta 不能為 NaN/Infinity");
    }

    @Transactional
    public double addScore(String username, double delta, @Nullable String reason) {
        ensureFinite(delta);
        User user = userRepo.findByUsername(username)
                            .orElseThrow(() -> new IllegalArgumentException("找不到使用者：" + username));

        long t0 = System.nanoTime();
        // 1) 寫 DB：事件 + 快照
        eventRepo.save(new ScoreEvent(user.getId(), delta, reason));
        userScoreRepo.upsertAndAdd(user.getId(), delta);
        Double dbScore = userScoreRepo.findScore(user.getId());
        double latest = dbScore == null ? 0d : dbScore;

        // 2) 寫 Redis（即時榜）
        try {
            redis.opsForZSet().incrementScore(rankKey, username, delta);
        } catch (Exception e) {
            // 不讓 DB 交易回滾（避免遺失真相），但打警告並交給對帳機制補救
            log.warn("ZINCRBY 失敗，稍後需對帳。user={}, delta={}, err={}", username, delta, e.toString());
        }

        log.info("addScore ok user={}, delta={}, newScore={}", username, delta, latest);
        log.debug("cost={}ms", (System.nanoTime()-t0)/1_000_000);
        return latest;
    }

    /** 取 Redis TopN（高→低） */
    public List<Rank> topN(int n) {
        if (n <= 0) return List.of();
        var tuples = redis.opsForZSet().reverseRangeWithScores(rankKey, 0, n - 1);
        if (tuples == null || tuples.isEmpty()) return List.of();

        List<Rank> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            String member = t.getValue();
            Double score  = t.getScore();
            if (member == null || score == null || !Double.isFinite(score)) continue;
            list.add(new Rank(member, score));
        }
        return list;
    }
}
