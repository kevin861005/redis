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
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AddScoreRequest req) {
        double newScore = rankService.addScore(req.getMember(), req.getDelta());
        return ResponseEntity.ok(Map.of("member", req.getMember(), "score", newScore));
    }

    @GetMapping("/top10")
    public ResponseEntity<List<Rank>> top10() {
        return ResponseEntity.ok(rankService.topN(10));
    }
}
