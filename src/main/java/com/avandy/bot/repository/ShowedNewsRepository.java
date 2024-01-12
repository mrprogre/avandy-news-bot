package com.avandy.bot.repository;

import com.avandy.bot.model.ShowedNews;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShowedNewsRepository extends JpaRepository<ShowedNews, Long> {

    @Query(value = "SELECT title_hash FROM showed_news WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findShowedNewsHashByChatId(Long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM showed_news WHERE add_date < (current_timestamp - cast('72 hours' as interval))",
            nativeQuery = true)
    int deleteOldShowedNews();

}