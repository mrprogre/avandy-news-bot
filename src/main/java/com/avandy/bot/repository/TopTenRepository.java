package com.avandy.bot.repository;

import com.avandy.bot.model.TopTenExcluded;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TopTenRepository extends JpaRepository<TopTenExcluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM top_ten_excluded " +
            "WHERE user_id = :chatId OR user_id IS NULL " +
            "ORDER BY id DESC",
            nativeQuery = true)
    Set<String> findAllExcludedFromTopTenByChatId(Long chatId);

}