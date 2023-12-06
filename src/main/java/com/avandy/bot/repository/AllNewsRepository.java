package com.avandy.bot.repository;

import com.avandy.bot.model.AllNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AllNewsRepository extends JpaRepository<AllNews, Long> {

    @Query(value = "SELECT title_hash FROM all_news WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findAllNewsHashByChatId(Long chatId);

}