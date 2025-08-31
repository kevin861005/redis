# SpringBoot 練習：Redis + PostgreSQL（Session / 排行榜 / 暫存 / Flyway）

> 利用 Spring 框架開發練習專案。設定檔皆為 `application.properties`；以 README 紀錄重點。  
> 目前功能涵蓋：**Redis 作為 Session 與快取**、**ZSET 排行榜**、**暫存 TTL**，並**整合 PostgreSQL + Flyway**（Schema 版本控管）。

---

## 架構概覽

- **Spring Boot**
  - Web API（/login, /me, /logout, /rank, /temp…）
  - Spring Data Redis（`StringRedisTemplate`）
  - Spring Data JPA（PostgreSQL）
  - Flyway（`db/migration` 管理資料庫 schema）
- **Redis（快取/Session/排行榜）**
  - Session Token：Value（TTL 30m，/me 成功可滑動續期）
  - 排行榜：ZSET（member=使用者、score=分數）
  - 暫存資料：Value（秒級 TTL）
- **PostgreSQL（真相來源）**
  - `users` 使用者
  - `score_events` 分數事件（稽核/回溯）
  - `user_scores` 即時計分 **快照**（查詢快）

```
Client ⇄ REST API (Spring Boot)
            ├─ Redis：Session / Rank / Temp
            └─ Postgres：Users / Scores（Flyway 管理）
```

---

## 快速開始

### 1) 啟動外部服務（Docker）
可將下列片段加入你的 `docker-compose.yml` 後啟動：
```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: redis-local
    ports: ["6379:6379"]
    command: ["redis-server","--appendonly","yes"]
    volumes: [ "redis_data:/data" ]
    healthcheck:
      test: ["CMD","redis-cli","PING"]
      interval: 5s
      timeout: 2s
      retries: 10

  postgres:
    image: postgres:16-alpine
    container_name: pg-local
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
      POSTGRES_DB: appdb
    ports: ["5432:5432"]   # 若被佔用改為 "5433:5432"，並同步調整 application.properties
    volumes: [ "pg_data:/var/lib/postgresql/data" ]
    healthcheck:
      test: ["CMD-SHELL","pg_isready -U app -d appdb"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  redis_data:
  pg_data:
```
啟動：`docker compose up -d`

### 2) application.properties（重點）
```properties
# --- Redis ---
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=3s

# --- PostgreSQL ---
spring.datasource.url=jdbc:postgresql://localhost:5432/appdb
spring.datasource.username=app
spring.datasource.password=app

# --- JPA/Hibernate ---
spring.jpa.hibernate.ddl-auto=validate  # 由 Flyway 管 schema，這裡用 validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# --- Flyway ---
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.clean-disabled=true
spring.flyway.validate-on-migrate=true

# --- 專案設定 ---
app.rank.key=rank:global
app.seed.enabled=true  # 啟動時自動塞開發用種子（可關閉）
```

---

## 資料庫遷移（Flyway）

- Migration 放在：`src/main/resources/db/migration/`
  - `V1__init.sql`：建立 `users`、`score_events`、`user_scores`
  - `V2__indexes.sql`：索引
  - （可依需要新增 `V3__...` 等）
- 需要相依：`flyway-core` + `flyway-database-postgresql`（Flyway 10 起 PostgreSQL 支援拆成模組）。

> 建議：`spring.jpa.hibernate.ddl-auto=validate`，避免 Hibernate 自動建表與 Flyway 打架。

---

## 開發用種子資料（可開關）

- `app.seed.enabled=true` 時，啟動會透過 `CommandLineRunner` 自動建立：
  - 使用者：`kevin`、`alice`、`bob`（密碼皆為 `test1234`，以 BCrypt 產生）
  - 初始分數：kevin=100、alice=80、bob=50（會同時寫 DB + Redis 排行）
- 已灌過或手動關閉開關時會自動跳過。

---

## API 介紹

### Auth（Session 快取）
- `POST /login`  
  驗證 DB 使用者與密碼成功後，產生 `token`，寫入 Redis：`session:token:{token}`，TTL=30 分鐘。  
  **回傳**：`{ "token": "..." }`
- `GET /me`  
  讀取 Redis token 取得使用者資訊；成功時**滑動續期** TTL（重設 30 分）。  
  **Header**：`Authorization: Bearer {token}`
- `POST /logout`  
  刪除 Redis 的 `session:token:{token}`。

### 排行榜（ZSET）
- `POST /rank/add`  
  Body：`{ "member": "kevin", "delta": 10, "reason": "quiz" }`  
  流程：DB 記錄事件 + 更新 `user_scores`（UPSERT 累加） → Redis `ZINCRBY`。  
  **回傳**：使用者最新分數。
- `GET /rank/top10`  
  由高到低取前 10 名，**回傳 DTO**：`[{ "member":"kevin", "score":100.0 }, ... ]`  
  （已使用 `Rank` 類別取代 `Map<String,Object>`）

> （可選）若有加入 **對帳 API**：`GET /rank/reconcile?n=10&epsilon=1e-6`，列出 DB vs Redis 的分數差異。

### 暫存資料（TTL）
- `POST /temp/put`  
  Body：`{ "key":"once", "value":"hello", "ttlSeconds":60 }`
- `GET /temp/get/{key}`  
  讀取暫存值；若不存在回 `null`。

---

## Postman 測試

可匯入（若你有建立）：
- **Collection**：`redis-practice-session.postman_collection.json`
- **Environment**：`redis-practice-local.postman_environment.json`

步驟：Import → 選環境 `redis-practice-local`（`{{baseUrl}}=http://localhost:8080`）→ 依序執行 Auth/Rank/Temp；`/login` 會自動把 `token` 寫入環境變數，後續請求已自動帶 `Authorization`。

---

## 重要技術點

- **StringRedisTemplate Bean**：使用 Lettuce 連線工廠；key/value 皆以字串序列化，方便在 CLI 檢視。
- **ZSetOperations**：有序集合操作；`incrementScore` 加分、`reverseRangeWithScores` 取 TopN。  
  - member 可以是字串或物件（取決於序列化）；**score 為 `double` 並且不可 `NaN/±Infinity`**。
- **NaN 防呆**：在 Service 入口驗證 `Double.isFinite(delta)`，避免 Redis 寫入失敗。

---

## 待辦（下一步練習）

- `/me` 成功讀取時的 TTL **滑動續期**（若尚未實作，請補）；
- 補上 **對帳 API** 與定期補償機制；
- 對 `/users/{name}` 增加 **cache-aside**（DB → Redis）與快取失效策略；
- 寫整合測試（Testcontainers：Redis + Postgres）。
