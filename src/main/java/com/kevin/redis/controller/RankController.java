package com.kevin.redis.controller;

import com.kevin.redis.dto.AddScoreRequest;
import com.kevin.redis.dto.Rank;
import com.kevin.redis.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rank")
public class RankController {

    private final RankService rankService;

    @PostMapping("/add")
    public Map<String,Object> add(@RequestBody Map<String,Object> req) {
        String member = (String) req.get("member");
        double delta = ((Number) req.get("delta")).doubleValue();
        double newScore = rankService.addScore(member, delta, (String) req.getOrDefault("reason","manual"));
        return Map.of("member", member, "newScore", newScore);
    }

    @GetMapping("/top{n}")
    public List<Rank> top(@PathVariable int n) {
        return rankService.topN(n);
    }
}
