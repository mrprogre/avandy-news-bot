-- Мой: 1254981379, Лена: 1020961767, Вика: 6455565758, Саша: 6128707071

select (select count(*) from excluding_terms) as excluded,
       (select count(*) from keywords)        as keywords,
       (select count(*) from showed_news)     as showed_news,
       (select count(*) from top_excluded)    as top_ten_excluded,
       (select count(*) from news_list)       as news_list,
       (select count(*) from rss_list)        as rss_list,
       (select count(*) from users)           as users;

-- Количество новостей, которые получили все пользователи
select case s.type when 2 then 'keys' else 'full' end as type,
       count(1)                                       as cnt,
       coalesce(u.first_name, u.user_name),
       s.chat_id
from showed_news s
         join users u
              on s.chat_id = u.chat_id
where s.add_date >= (current_timestamp - cast('1 hours' as interval))
--and s.chat_id = 678952524
group by s.chat_id, u.user_name, u.first_name, type
order by type desc, cnt desc;

-- Новости, которые получил пользователь
select source,
       title,
       pub_date::time,
       n.add_date::time,
       extract(minute from (n.pub_date - n.add_date)) as "pub-add",
       s.add_date
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 153312670 -- 900: 678952524, 257963162, 153312670
where n.add_date >= (current_timestamp - cast('12 hours' as interval))
order by n.add_date desc;

-- Новости по языкам
SELECT count(*) --n.id, n.source, n.title, n.title_hash, n.link, n.pub_date, n.add_date
FROM news_list n
         JOIN rss_list r on n.source = r.source and lang = 'ru'
WHERE pub_date >= current_date;

-- Новости по источнику
select *
from news_list
where source = 'EL PAÍS'
  and pub_date > (current_timestamp - cast('2 hours' as interval));

-- Количество новостей, сохранённых за последние 6 часов
SELECT count(*)
FROM news_list
WHERE add_date > (current_timestamp - cast('6 hours' as interval));

-- Очистить full search
--select count(*)
delete
from showed_news
where chat_id = 1254981379
  and type = 4;

-- топ ключевых слов по всем пользователям
select keyword, count(keyword) cnt
from keywords
group by keyword
having count(keyword) > 2
order by cnt desc;

-- количество ключевых слов по пользователям
select * from keywords where chat_id = 5294960396;
select chat_id, count(keyword) cnt
from keywords
group by chat_id
having count(keyword) > 2
order by cnt desc;

-- количество слов-исключений
select * from excluding_terms where chat_id = 5294960396;
select chat_id, count(word) cnt
from excluding_terms
group by chat_id
order by cnt desc;


