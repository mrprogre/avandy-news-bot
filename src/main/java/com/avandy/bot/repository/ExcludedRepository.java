package com.avandy.bot.repository;

import com.avandy.bot.model.Excluded;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExcludedRepository extends CrudRepository<Excluded, Long> {

    @Query(value = "SELECT word FROM excluded WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findExcludedByChatId(Long chatId);

    @Query(value = "SELECT word FROM excluded WHERE chat_id = :chatId ORDER BY id DESC LIMIT 30", nativeQuery = true)
    List<String> findExcludedByChatIdLimit(Long chatId);

    @Query(value = "SELECT count(word) FROM excluded WHERE chat_id = :chatId", nativeQuery = true)
    int getExcludedCountByChatId(Long chatId);

    @Query(value = "SELECT count(word) FROM excluded WHERE lower(word) = lower(:word)", nativeQuery = true)
    int isWordExists(String word);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM excluded WHERE chat_id = :chatId AND lower(word) = lower(:word)", nativeQuery = true)
    void deleteExcludedByChatId(Long chatId, String word);
}