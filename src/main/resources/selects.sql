select u.chat_id,
       u.first_name,
       u.user_name,
       k.keyword,
       e.word,
       s.period,
       s.period_all,
       s.scheduler,
       s.excluded
from users u
         left join settings s on u.chat_id = s.chat_id
         left join keywords k on u.chat_id = k.chat_id
         left join excluded e on u.chat_id = e.chat_id
where u.chat_id != 1254981379;

-- todo Сделать тесты на каждый кейс в идеале
-- todo Сделать поиск по ключевым словам с %, чтобы искать типа повыш%зарплат'
-- todo проверка заблокирован ли бот юзером

select n.source, n.title, extract(minute from (n.pub_date - n.add_date)) as "pub-add"
from news_list n
         join showed_news s
              on n.title_hash = s.title_hash and s.chat_id = 6190696388
order by n.id desc;
