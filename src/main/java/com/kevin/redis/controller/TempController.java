package com.kevin.redis.controller;

import com.kevin.redis.dto.PutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * 暫存資料（Temporary Cache）API 控制器。
 *
 * <p>用途：
 * <ul>
 *   <li>以 Redis 作為 <b>短期 Key-Value 暫存</b> 的存放區，使用 {@code StringRedisTemplate} 的
 *       {@code ValueOperations} 進行存取，並透過 TTL（Time To Live）控制有效期。</li>
 *   <li>典型情境：Email/SMS 驗證碼、一段時間內有效的一次性 Token、表單分步暫存、短期快取等。</li>
 * </ul>
 *
 * <p>提供端點：
 * <ul>
 *   <li>{@code POST /temp/put}：寫入暫存資料（需提供 {@code key}、{@code value} 與 {@code ttlSeconds}）。</li>
 *   <li>{@code GET  /temp/get/{key}}：讀取暫存資料；若 key 不存在則回傳 value 為 {@code null}。</li>
 * </ul>
 *
 * <p>Redis Key 命名：
 * <ul>
 *   <li>Key 前綴由設定檔 {@code app.temp.prefix} 控制（預設 {@code "temp:"}）。</li>
 *   <li>實際寫入的鍵名為：{@code temp:{key}}，例如 {@code temp:once}。</li>
 * </ul>
 *
 * <p>注意事項：
 * <ul>
 *   <li>本範例僅示範最小可行功能，未加入權限驗證、輸入長度/內容限制與併發控制。</li>
 *   <li>不建議存放敏感或長期資料，請依需求設定合理的 TTL。</li>
 *   <li>若需觀察剩餘壽命，可搭配 Redis 指令 {@code TTL temp:{key}} 或在服務端加上查詢 TTL 的 API。</li>
 * </ul>
 *
 * <p>相關類別：
 * <ul>
 *   <li>{@link org.springframework.data.redis.core.StringRedisTemplate}</li>
 * </ul>
 *
 * @author kevin_chen
 * @since 2025/08/28
 */
@RestController
@RequestMapping("/temp")
@RequiredArgsConstructor
public class TempController {

    private final StringRedisTemplate redis;

    @Value("${app.temp.prefix:temp:}")
    private String tempPrefix;

    private String keyOf(String key) { return tempPrefix + key; }

    @PostMapping("/put")
    public ResponseEntity<?> put(@RequestBody PutRequest req) {
        redis.opsForValue().set(keyOf(req.getKey()), req.getValue(), Duration.ofSeconds(req.getTtlSeconds()));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/get/{key}")
    public ResponseEntity<?> get(@PathVariable String key) {
        String val = redis.opsForValue().get(keyOf(key));
        return ResponseEntity.ok(Map.of("key", key, "value", val));
    }
}
