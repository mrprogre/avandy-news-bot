select u.chat_id,
       u.first_name,
       string_agg(distinct k.keyword, ',' order by k.keyword) as keyword,
       string_agg(distinct e.word, ',' order by e.word)       as excluding,
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
where pub_date > (current_timestamp - interval '1440 minutes')::timestamp
order by n.id desc;

-- я 1254981379, Лена 1020961767, Вика 6455565758, Саша 6128707071
-- todo Сделать тесты на каждый кейс в идеале
-- todo Сделать поиск по ключевым словам с %, чтобы искать типа повыш%зарплат'

SELECT title FROM news_list
WHERE pub_date > (current_timestamp - cast('8h' as interval))
-- --and lower(title) ~ '^сво | сво | сво$'
AND title ilike '%заявил%'
except
SELECT title FROM news_list
WHERE pub_date > (current_timestamp - cast('8h' as interval))
  and lower(title) ~ '^заявил\s|\sзаявил\s|\sзаявил$'
;

-- ALLERT
-- private void showAlert(String callbackQueryId, String text) {
--         AnswerCallbackQuery answer = new AnswerCallbackQuery();
-- answer.setCallbackQueryId(callbackQueryId);
-- answer.setText(text);
-- answer.setShowAlert(true);
--
-- try {
-- execute(answer);
-- } catch (TelegramApiException e) {
--             log.error(e.getMessage());
-- }
-- }