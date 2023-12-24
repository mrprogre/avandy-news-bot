package com.avandy.bot.repository;

import com.avandy.bot.model.Excluded;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExcludedRepository extends CrudRepository<Excluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM excluded WHERE chat_id = :chatId", nativeQuery = true)
    List<String> findExcludedByChatId(Long chatId);

    @Query(value = "SELECT lower(word) as word FROM excluded WHERE chat_id = :chatId ORDER BY id DESC LIMIT :limit",
            nativeQuery = true)
    List<String> findExcludedByChatIdLimit(Long chatId, int limit);

    @Query(value = "SELECT count(word) FROM excluded WHERE chat_id = :chatId", nativeQuery = true)
    int getExcludedCountByChatId(Long chatId);

    @Query(value = "SELECT count(word) FROM excluded WHERE lower(word) = lower(:word) and chat_id = :chatId"
            , nativeQuery = true)
    int isWordExists(Long chatId, String word);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM excluded WHERE chat_id = :chatId AND lower(word) = lower(:word)", nativeQuery = true)
    void deleteExcludedByChatId(Long chatId, String word);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM excluded WHERE chat_id = :chatId", nativeQuery = true)
    void deleteAllExcludedByChatId(Long chatId);
}