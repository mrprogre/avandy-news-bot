package com.avandy.bot.repository;

import com.avandy.bot.model.ShowedNews;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShowedNewsRepository extends JpaRepository<ShowedNews, Long> {

    @Query(value = "select title_hash from showed_news where chat_id = :chatId", nativeQuery = true)
    List<String> findShowedNewsHashByChatId(Long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from showed_news where " +
            "(type = 2 and add_date < (current_timestamp - cast('72 hours' as interval))) or" +
            "(type = 4 and add_date < (current_timestamp - cast('24 hours' as interval)))", nativeQuery = true)
    int deleteOldNews();

}