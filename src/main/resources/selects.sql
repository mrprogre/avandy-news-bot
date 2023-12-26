SET TIME ZONE 'Europe/Moscow';
select current_timestamp;

select title, pub_date
from news_list n
where pub_date > (current_timestamp + cast('1 hours' as interval))
  AND title ilike '%суд%'
order by pub_date desc;

select u.chat_id,
       u.first_name,
       u.user_name,
       k.keyword,
       e.word       as exclude,
       s.period,
       s.period_all as "all",
       s.period_top as top,
       s.scheduler  as sched,
       s.excluded   as excl
from users u
         left join settings s on u.chat_id = s.chat_id
         left join keywords k on u.chat_id = k.chat_id
         left join excluded e on u.chat_id = e.chat_id
where u.chat_id != 1254981379;

select source, title, pub_date, extract(minute from (pub_date - add_date)) as "pub-add"
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 6190696388
where pub_date > (current_timestamp - interval '180 minutes')::timestamp
order by n.id desc;

-- мой id 1254981379
-- todo сделать проще удаление ключевых слов
-- todo Сделать тесты на каждый кейс в идеале
-- todo Сделать поиск по ключевым словам с %, чтобы искать типа повыш%зарплат'