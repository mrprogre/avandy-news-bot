package com.avandy.bot.repository;

import com.avandy.bot.model.ShowedNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShowedNewsRepository extends JpaRepository<ShowedNews, Long> {

    @Query(value = "SELECT title_hash FROM showed_news WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findShowedNewsHashByChatId(Long chatId);

}