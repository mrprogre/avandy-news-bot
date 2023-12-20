package com.avandy.bot.repository;

import com.avandy.bot.model.TopTenExcluded;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TopTenRepository extends JpaRepository<TopTenExcluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM top_ten_excluded " +
            "WHERE user_id = :chatId OR user_id IS NULL " +
            "ORDER BY id DESC",
            nativeQuery = true)
    Set<String> findAllExcludedFromTopTenByChatId(Long chatId);

    @Query(value = "SELECT count(word) FROM top_ten_excluded WHERE lower(word) = lower(:keyword) and user_id = :chatId",
            nativeQuery = true)
    int isWordExists(Long chatId, String keyword);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM top_ten_excluded WHERE user_id = :chatId AND lower(word) = lower(:word)",
            nativeQuery = true)
    void deleteWordByChatId(Long chatId, String word);

}