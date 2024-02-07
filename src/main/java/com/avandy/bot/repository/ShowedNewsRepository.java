package com.avandy.bot.repository;

import com.avandy.bot.model.ShowedNews;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.stream.Stream;

public interface ShowedNewsRepository extends JpaRepository<ShowedNews, Long> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from showed_news where " +
            "(type = 2 and add_date < (current_timestamp - cast('72 hours' as interval))) or" +
            "(type = 4 and add_date < (current_timestamp - cast('24 hours' as interval)))", nativeQuery = true)
    int deleteOldNews();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from showed_news where type = 4 and chat_id = :chatId", nativeQuery = true)
    void deleteHistoryManual(long chatId);

    // Получить массив из 2 значений
    @Query(value = "select title_hash, type from showed_news where chat_id = :chatId", nativeQuery = true)
    Stream<Object[]> findShowedNewsHashByChatId(long chatId);

    // Объединяем полученные ранее значения в строки
    @Transactional
    default List<String> findHashAndType(long chatId) {
        return findShowedNewsHashByChatId(chatId)
                .map(x -> x[0].toString() + x[1].toString())
                .toList();
    }



}