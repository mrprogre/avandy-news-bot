delete from showed_news where chat_id = 1254981379 and type = 4; -- full search

-- Данные пользователей
-- Мой: 1254981379, Лена: 1020961767, Вика: 6455565758, Саша: 6128707071
select u.chat_id,
       u.first_name,
       rpad(s.period, 5, ' ') ||rpad(s.period_all, 3, ' ')||'  '||s.period_top                                       as "key-all-top",
       rpad(s.scheduler, 3, ' ')||' '||rpad(s.excluded, 3, ' ')|| ' '|| s.lang as "sch-exc",
       --s.start,
       string_agg(distinct k.keyword, ',' order by k.keyword) as keyword,
       string_agg(distinct t.word, ',' order by t.word)       as top,
       string_agg(distinct e.word, ',' order by e.word)       as excluding
from users u
         left join settings s on u.chat_id = s.chat_id
         left join keywords k on u.chat_id = k.chat_id
         left join excluding_terms e on u.chat_id = e.chat_id
         left join top_excluded t on u.chat_id = t.chat_id
where u.chat_id not in (1254981379 /* я */ /*,1020961767 /* Лена */, 6455565758 /* Вика */, 6128707071 /* Саша */*/)
  and u.is_active = 1
group by u.chat_id, u.first_name, s.period, s.period_all, s.scheduler, s.start, s.excluded, s.lang, s.period_top,
         u.is_active;

-- Новости, которые получил пользователь
select source, title, pub_date::time, n.add_date::time, extract(minute from (n.pub_date - n.add_date)) as "pub-add"
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 975260763
where n.add_date >= '2024-01-17 12:00:00'::timestamp
order by n.id desc;

-- Rows count
-- 05.01.2024: 2747, 25, 25470, 32, 5291, 205, 9
select (select count(*) from excluding_terms) as excluded,
       (select count(*) from keywords)        as keywords,
       (select count(*) from news_list)       as news_list,
       (select count(*) from rss_list)        as rss_list,
       (select count(*) from showed_news)     as showed_news,
       (select count(*) from top_excluded)    as top_ten_excluded,
       (select count(*) from users)           as users;

-- Новости по языкам
SELECT n.id, n.source, n.title, n.title_hash, n.link, n.pub_date, n.add_date
FROM news_list n
         JOIN rss_list r on n.source = r.source and lang = 'de'
WHERE pub_date > (current_timestamp - cast('12 hours' as interval));

-- Новости по источнику
select *
from news_list
where source = 'EL PAÍS'
  and pub_date > (current_timestamp - cast('2 hours' as interval));

-- Количество новостей, сохранённых за последние 6 часов
SELECT count(*)
FROM news_list
WHERE pub_date > (current_timestamp - cast('6 hours' as interval));