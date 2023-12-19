package com.avandy.bot.repository;

import com.avandy.bot.model.Keyword;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface KeywordRepository extends CrudRepository<Keyword, Long> {
    @Query(value = "SELECT id, chat_id, lower(keyword) as keyword FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    List<Keyword> findAllByChatId(Long chatId);

    @Query(value = "SELECT lower(keyword) as keyword FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findKeywordsByChatId(Long chatId);

    @Query(value = "SELECT count(keyword) FROM keywords WHERE lower(keyword) = lower(:keyword) and chat_id = :chatId",
            nativeQuery = true)
    int isKeywordExists(Long chatId, String keyword);

    @Query(value = "SELECT count(keyword) FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    int getKeywordsCountByChatId(Long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM keywords WHERE chat_id = :chatId AND lower(keyword) = lower(:word)", nativeQuery = true)
    void deleteKeywordByChatId(Long chatId, String word);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    void deleteAllKeywordsByChatId(Long chatId);
}
