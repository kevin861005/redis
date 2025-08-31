package com.kevin.redis;

import com.kevin.redis.persistence.model.User;
import com.kevin.redis.persistence.repository.UserRepository;
import com.kevin.redis.service.RankService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RankService rankService;

    @Value ("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("[seed] disabled by config");
            return;
        }
        if (users.count() > 0) {
            log.info("[seed] skipped (users already exist)");
            return;
        }

        // 1) 建三個練習用帳號（密碼 test1234）
        users.save(newUser("kevin", "Kevin", "test1234"));
        users.save(newUser("alice", "Alice", "test1234"));
        users.save(newUser("bob",   "Bob",   "test1234"));

        // 2) 造幾筆初始分數（會寫 DB + Redis）
        rankService.addScore("kevin", 100, "seed");
        rankService.addScore("alice",  80, "seed");
        rankService.addScore("bob",    50, "seed");

        log.info("[seed] done: users=kevin/alice/bob (pw=test1234) + initial scores");
    }

    private User newUser(String uname, String display, String rawPw) {
        User u = new User();
        u.setUsername(uname);
        u.setDisplayName(display);
        u.setPasswordHash(encoder.encode(rawPw));
        u.setRole("USER");
        return u;
    }
}
