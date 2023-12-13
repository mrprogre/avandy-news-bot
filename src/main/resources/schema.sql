create table if not exists rss_list
(
    id        bigint generated by default as identity primary key,
    chat_id   bigint,
    source    varchar(64),
    link      varchar(512),
    is_active integer default 1,
    position  INTEGER default 100,
    add_date  timestamp,
    country   varchar(128),
    constraint ui_rss_list_chat_id_link unique (chat_id, link)
);

create table if not exists users
(
    chat_id       bigint,
    first_name    varchar(64),
    last_name     varchar(64),
    user_name     varchar(32),
    registered_at timestamp,
    is_active     integer default 1,
    constraint pk_users_chat_id primary key (chat_id)
);

create table if not exists settings
(
    chat_id    bigint,
    period     varchar(8) default '12h'::text,
    period_all varchar(8) default '1h'::text,
    scheduler  varchar(3) default 'on'::text,
    start      time       default '14:00'::time,
    excluded   varchar(3) default 'on'::text,
    constraint fk_settings_chat_id foreign key (chat_id) references users (chat_id) on delete cascade
);

comment on column settings.period is 'Глубина поиска по ключевым словам';
comment on column settings.period_all is 'Глубина поиска всех новостей';
comment on column settings.scheduler is 'Включение/выключение автопоиска (on/off)';
comment on column settings.start is 'Время старта автопоиска по ключевым словам. Исходя из него формируется список часов старта поиска.';

create table if not exists keywords
(
    id      bigint generated by default as identity primary key,
    chat_id bigint,
    keyword varchar(64),
    constraint fk_keywords_chat_id foreign key (chat_id) references users (chat_id) on delete cascade,
    constraint ui_keywords_chat_id_link unique (chat_id, keyword)
);

create table if not exists excluded
(
    id      bigint generated by default as identity primary key,
    word    varchar(32),
    chat_id bigint,
    constraint fk_excluded_chat_id foreign key (chat_id) references users (chat_id) on delete cascade,
    constraint ui_excluded unique (chat_id, word)
);

create table if not exists all_news
(
    id         bigint generated by default as identity primary key,
    chat_id    bigint,
    title_hash varchar(10),
    type       integer,
    constraint fk_headlines_chat_id foreign key (chat_id) references users (chat_id) on delete cascade,
    constraint ui_all_news unique (chat_id, title_hash, type)
);

create table if not exists todo
(
    id       integer generated by default as identity primary key,
    chat_id  bigint,
    text     varchar(512),
    add_date date,
    constraint fk_todo_chat_id foreign key (chat_id) references users (chat_id) on delete cascade
);

create table if not exists news_list
(
    id         bigint generated by default as identity primary key,
    source     varchar(64),
    title      varchar(512),
    title_hash varchar(10),
    link       varchar(512),
    pub_date   timestamp,
    add_date   timestamp default current_timestamp::timestamp,
    constraint ui_news_list unique (title_hash)
);