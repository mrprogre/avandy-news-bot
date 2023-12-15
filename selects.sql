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