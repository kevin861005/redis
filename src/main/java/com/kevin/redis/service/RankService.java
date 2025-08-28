package com.kevin.redis.service;

import com.kevin.redis.dto.Rank;
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

    /**
     * 排行榜 ZSET 的 key；可由 application.properties 透過 app.rank.key 覆蓋，預設值為 "rank:global"。
     */
    @Value("${app.rank.key:rank:global}")
    private String rankKey;

    /**
     * 增加成員分數；若成員不存在則建立並賦初值。
     * <p>底層對應 Redis：ZINCRBY。</p>
     *
     * @param member
     *         成員（例如 username 或 userId）
     * @param delta
     *         要增加的分數（可正可負；不可為 NaN）
     *
     * @return 增分後的最新分數（若底層回傳 null，則回 0）
     */
    public double addScore(String member, double delta) {
        if (Double.isNaN(delta)) {
            log.warn("增分失敗，delta 不能為 NaN，member={}", member);
            throw new IllegalArgumentException("delta 不能為 NaN");
        }

        long start = System.nanoTime();
        log.info("更新排行榜分數開始, key={}, member={}, delta={}", rankKey, member, delta);
        Double newScore = redis.opsForZSet().incrementScore(rankKey, member, delta);
        double result = newScore == null ? 0d : newScore;
        long costMs = (System.nanoTime() - start) / 1_000_000;

        log.info("更新排行榜分數完成, member={}, newScore={}, cost={}ms", member, result, costMs);
        log.debug("ZINCRBY 明細: key={}, member={}, delta={}, newScore={}", rankKey, member, delta, result);
        return result;
    }

    /**
     * 取得前 N 名（分數由高到低），並回傳 member 與 score 清單。
     * <p>底層對應 Redis：ZRANGE key 0 (n-1) REV WITHSCORES（6.2+ 的建議語法；
     * Spring 封裝方法為 {@code reverseRangeWithScores}）。</p>
     *
     * @param n 要取的名次數量（n <= 0 時回空清單）
     * @return 每筆 Map 內容：
     * <ul>
     *   <li>member：成員（字串）</li>
     *   <li>score：分數（Double）</li>
     * </ul>
     */
    public List<Rank> topN(int n) {
        if (n <= 0) {
            log.warn("查詢 TopN 參數異常，n 必須 > 0，收到 n={}", n);
            return List.of();
        }

        long start = System.nanoTime();
        log.info("查詢排行榜 TopN 開始, key={}, n={}", rankKey, n);

        /**
         * 0, n-1 代表取出 前 n 筆（因為是 index，從 0 開始）
         * reverseRangeWithScores = 由大到小取出，並且帶上 score（分數）
         */
        Set<ZSetOperations.TypedTuple<String>> set = redis.opsForZSet().reverseRangeWithScores(rankKey, 0, n - 1);
        List<Rank> list = new ArrayList<>();
        if (set == null || set.isEmpty()) {
            log.info("查無排行資料或 ZSET 尚未建立, key={}", rankKey);
            return list;
        }

        for (ZSetOperations.TypedTuple<String> t : set) {
            Rank rank = Rank.builder()
                            .member(t.getValue())   // ZSET 的 member
                            .score(t.getScore())    // ZSET 的 score (Double)
                            .build();

            list.add(rank);
        }

        long costMs = (System.nanoTime() - start) / 1_000_000;
        log.info("查詢排行榜 TopN 完成, key={}, n={}, 回傳筆數={}, cost={}ms", rankKey, n, list.size(), costMs);
        log.debug("TopN 明細: {}", list);
        return list;
    }
}
