package com.avandy.bot.repository;

import com.avandy.bot.model.TopExcluded;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface TopTenRepository extends JpaRepository<TopExcluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM top_excluded WHERE chat_id = :chatId ORDER BY id DESC",
            nativeQuery = true)
    Set<String> findAllExcludedFromTopTenByChatId(Long chatId);

    @Query(value = "SELECT count(word) FROM top_excluded WHERE lower(word) = lower(:keyword) and chat_id = :chatId",
            nativeQuery = true)
    int isWordExists(Long chatId, String keyword);

    @Query(value = "SELECT count(word) FROM top_excluded WHERE chat_id = :chatId", nativeQuery = true)
    int deleteFromTopTenCount(Long chatId);

    @Query(value = "SELECT word FROM top_excluded WHERE chat_id = :chatId order by id desc offset :offset limit :limit",
            nativeQuery = true)
    List<String> findExcludedWordsPage(Long chatId, int offset, int limit);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM top_excluded WHERE chat_id = :chatId AND lower(word) = lower(:word)",
            nativeQuery = true)
    void deleteWordByChatId(Long chatId, String word);

}