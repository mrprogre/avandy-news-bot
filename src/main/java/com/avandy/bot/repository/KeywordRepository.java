package com.avandy.bot.repository;

import com.avandy.bot.model.Keyword;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface KeywordRepository extends CrudRepository<Keyword, Long> {
    @Query(value = "SELECT id, chat_id, keyword FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    List<Keyword> findAllByChatId(Long chatId);

    @Query(value = "SELECT keyword FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findKeywordsByChatId(Long chatId);

    @Query(value = "SELECT count(keyword) FROM keywords WHERE keyword = :keyword", nativeQuery = true)
    int isKeywordExists(String keyword);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM keywords WHERE chat_id = :chatId AND keyword = :word", nativeQuery = true)
    void deleteKeywordByChatId(Long chatId, String word);

    @Query(value = "SELECT count(keyword) FROM keywords WHERE chat_id = :chatId", nativeQuery = true)
    int getKeywordsCountByChatId(Long chatId);
}
