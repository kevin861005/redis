package com.kevin.redis.service;

import com.kevin.redis.persistence.repository.UserScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RankReconcileService {
    private final StringRedisTemplate redis;
    private final UserScoreRepository userScoreRepo;

    @Value ("${app.rank.key:rank:global}")
    private String rankKey;

    /** 回傳分數不一致的清單（|差值| > epsilon） */
    public List<Map<String,Object>> diffTopN(int n, double epsilon) {
        // Redis 端
        var rs = redis.opsForZSet().reverseRangeWithScores(rankKey, 0, n-1);
        Map<String, Double> redisMap = new LinkedHashMap<>();
        if (rs != null) for (var t : rs) redisMap.put(t.getValue(), t.getScore());

        // DB 端
        Map<String, Double> dbMap = new LinkedHashMap<>();
        for (var row : userScoreRepo.topN(n)) {
            dbMap.put(row.getUsername(), row.getScore());
        }

        // 合併比對
        Set<String> names = new LinkedHashSet<>();
        names.addAll(dbMap.keySet());
        names.addAll(redisMap.keySet());

        List<Map<String,Object>> diffs = new ArrayList<>();
        for (String name : names) {
            double a = dbMap.getOrDefault(name, 0d);
            double b = redisMap.getOrDefault(name, 0d);
            if (Math.abs(a - b) > epsilon) {
                diffs.add(Map.of("member", name, "db", a, "redis", b, "delta", b - a));
            }
        }
        return diffs;
    }
}
