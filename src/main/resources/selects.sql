select u.chat_id,
       u.first_name,
       string_agg(k.keyword, ',' ORDER BY k.keyword) as keyword,
       string_agg(e.word, ',' ORDER BY e.word)       as excluding,
       s.period                                      as "key",
       s.period_all                                  as "all",
       s.period_top                                  as "top",
       s.scheduler,
       s.excluded                                    as "excl",
       s.lang,
       u.is_active                                   as "1/0",
       s.start
from users u
         left join settings s on u.chat_id = s.chat_id
         left join keywords k on u.chat_id = k.chat_id
         left join excluded e on u.chat_id = e.chat_id
where
--u.chat_id in (5184241058, 975260763)
u.chat_id not in (1254981379 /* я */ /*,1020961767 /* Лена */, 6455565758 /* Вика */, 6128707071 /* Саша */*/)
group by u.chat_id, u.first_name, s.period, s.period_all, s.scheduler, s.start, s.excluded, s.lang, s.period_top,
         u.is_active
;

select title, pub_date
from news_list n
where pub_date > (current_timestamp + cast('1 hours' as interval))
  AND title ilike '%суд%'
order by pub_date desc;

select source, title, pub_date, extract(minute from (pub_date - add_date)) as "pub-add"
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 6190696388
where pub_date > (current_timestamp - interval '180 minutes')::timestamp
order by n.id desc;

-- мой id 1254981379
-- todo Сделать тесты на каждый кейс в идеале
-- todo Сделать поиск по ключевым словам с %, чтобы искать типа повыш%зарплат'