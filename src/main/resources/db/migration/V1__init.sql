-- 使用者
create table users (
                       id            bigserial primary key,
                       username      varchar(50) not null unique,
                       password_hash varchar(100) not null,
                       display_name  varchar(100) not null,
                       role          varchar(20)  not null default 'USER',
                       created_at    timestamptz  not null default now(),
                       updated_at    timestamptz  not null default now()
);

-- 分數事件（累計到 Redis 排行；DB 做真相與稽核）
create table score_events (
                              id         bigserial primary key,
                              user_id    bigint not null references users(id),
                              delta      double precision not null,
                              reason     varchar(100),
                              created_at timestamptz not null default now()
);

-- 可選：即時計分快照（彙總）
create table user_scores (
                             user_id bigint primary key references users(id),
                             score   double precision not null default 0
);