package com.avandy.bot.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonQuery {
    @PersistenceContext
    EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void getShowedNewsByPeriod() {
        var rows = entityManager.createNativeQuery(
                "select s.chat_id,  " +
                        "       coalesce(u.first_name, u.user_name),  " +
                        "       case s.type when 2 then 'keys' else 'full' end as type,  " +
                        "       count(1)                                       as cnt  " +
                        "from showed_news s  " +
                        "         join users u on s.chat_id = u.chat_id  " +
                        "where s.add_date > (current_timestamp - cast('12 hours' as interval))  " +
                        "group by s.chat_id, u.user_name, u.first_name, type  " +
                        "order by type desc, cnt desc")
                .getResultList();

        for (Object row : rows) {
            try {
                log.warn(objectMapper.writeValueAsString(row));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
    }

}