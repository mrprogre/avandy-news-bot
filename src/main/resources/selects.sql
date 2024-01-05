-- Данные пользователей
-- Мой: 1254981379, Лена: 1020961767, Вика: 6455565758, Саша: 6128707071
select u.chat_id,
       u.first_name,
       string_agg(distinct k.keyword, ',' order by k.keyword) as keyword,
       string_agg(distinct e.word, ',' order by e.word)       as excluding,
       s.period                                               as "key",
       s.period_all                                           as "all",
       s.period_top                                           as "top",
       s.scheduler,
       s.excluded                                             as "excl",
       s.lang,
       u.is_active                                            as "1/0",
       s.start
from users u
         left join settings s on u.chat_id = s.chat_id
         left join keywords k on u.chat_id = k.chat_id
         left join excluded e on u.chat_id = e.chat_id
where u.chat_id not in (1254981379 /* я */ /*,1020961767 /* Лена */, 6455565758 /* Вика */, 6128707071 /* Саша */*/)
group by u.chat_id, u.first_name, s.period, s.period_all, s.scheduler, s.start, s.excluded, s.lang, s.period_top,
         u.is_active;

-- Rows count
-- 05.01.2024: 2747, 25, 25470, 32, 5291, 205, 9
select (select count(*) from excluded)         as excluded,
       (select count(*) from keywords)         as keywords,
       (select count(*) from news_list)        as news_list,
       (select count(*) from rss_list)         as rss_list,
       (select count(*) from showed_news)      as showed_news,
       (select count(*) from top_ten_excluded) as top_ten_excluded,
       (select count(*) from users)            as users;

-- Новости, которые получил пользователь
select source, title, pub_date, extract(minute from (pub_date - add_date)) as "pub-add"
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 333808055
where pub_date > '2024-01-04'::date
order by n.id desc;