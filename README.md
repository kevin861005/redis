# Redis Practice Session

## 目標
- 快取 / Session / 排行榜（ZSet）/ 暫存 TTL 一次練熟。

## 如何啟動
1. 啟 Redis：`docker compose up -d`

## 重要設定
- `application.properties` 使用 `spring.data.redis.*`
- Session TTL：`app.session.ttl-minutes=30`
- ZSet key：`app.rank.key=rank:global`
- 暫存前綴：`app.temp.prefix=temp:`

## API
- `POST /login`：寫入 `session:token:{token}`（TTL）
- `GET /me`：讀 token 取得使用者資訊
- `POST /logout`：刪 token
- `POST /rank/add`：ZSet 累計分數
- `GET /rank/top10`：ZSet 前 10 名
- `POST /temp/put`、`GET /temp/get/{key}`：Value + TTL

## Redis 命令 ↔ Spring Data Redis 對照表

| 情境                | Redis 命令（例）                             | Spring Data Redis                                                |
|-------------------|-----------------------------------------|------------------------------------------------------------------|
| 寫入 Session（含 TTL） | `SET session:token:{t} <json> EX 1800`  | `opsForValue().set(key, json, Duration.ofMinutes(ttl))`          |
| 讀取 Session        | `GET session:token:{t}`                 | `opsForValue().get(key)`                                         |
| 刪除 Token          | `DEL session:token:{t}`                 | `delete(key)`                                                    |
| 取 TTL             | `TTL session:token:{t}`                 | `getExpire(key, TimeUnit.SECONDS)`                               |
| 續期                | `EXPIRE session:token:{t} 1800`         | `expire(key, Duration.ofMinutes(ttl))`                           |
| 排行加分              | `ZINCRBY rank:global 10 kevin`          | `opsForZSet().incrementScore(rankKey, "kevin", 10)`              |
| 取前 N 名（高→低）       | `ZRANGE rank:global 0 N REV WITHSCORES` | `reverseRangeWithScores(rankKey, 0, N-1)`                        |
| 暫存值（60 秒）         | `SET temp:once hello EX 60`             | `opsForValue().set("temp:once","hello", Duration.ofSeconds(60))` |

> 備註：Redis 6.2+ 建議用 `ZRANGE ... REV WITHSCORES` 取代舊 `ZREVRANGE ... WITHSCORES`。

### redis-cli 快速測試
- Bash（macOS/Linux/git-bash）
  ```bash
  chmod +x redis-cli-quicktest.sh
  ./redis-cli-quicktest.sh
  ```
- Windows PowerShell
  ```powershell
  .\redis-cli-quicktest.ps1
  ```

上面腳本會依序示範：`SET ... EX` / `GET` / `TTL`、`ZINCRBY`、`ZRANGE ... REV WITHSCORES`，並在最後清掉測試資料。