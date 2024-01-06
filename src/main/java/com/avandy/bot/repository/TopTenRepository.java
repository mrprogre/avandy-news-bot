package com.avandy.bot.repository;

import com.avandy.bot.model.TopExcluded;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TopTenRepository extends JpaRepository<TopExcluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM top_excluded " +
            "WHERE chat_id = :chatId OR chat_id IS NULL " +
            "ORDER BY id DESC",
            nativeQuery = true)
    Set<String> findAllExcludedFromTopTenByChatId(Long chatId);

    @Query(value = "SELECT count(word) FROM top_excluded WHERE lower(word) = lower(:keyword) and chat_id = :chatId",
            nativeQuery = true)
    int isWordExists(Long chatId, String keyword);

    @Query(value = "SELECT count(word) FROM top_excluded WHERE chat_id = :chatId", nativeQuery = true)
    int deleteFromTopTenCount(Long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM top_excluded WHERE chat_id = :chatId AND lower(word) = lower(:word)",
            nativeQuery = true)
    void deleteWordByChatId(Long chatId, String word);

}