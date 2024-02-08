package com.avandy.bot.repository;

import com.avandy.bot.model.RssList;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RssRepository extends CrudRepository<RssList, Integer> {
    @Query(value = "SELECT DISTINCT source FROM rss_list WHERE is_active = 1 " +
            "AND (lang = :lang or chat_id = :chatId) ORDER BY source", nativeQuery = true)
    List<String> findAllActiveSources(String lang, long chatId);

    @Query(value = "SELECT source FROM rss_list WHERE is_active = 1 and chat_id = :chatId ORDER BY source",
            nativeQuery = true)
    List<String> findPersonalSources(long chatId);

    @Query(value = "SELECT 1 FROM rss_list WHERE chat_id = :chatId and link = :link", nativeQuery = true)
    Integer isExistsByChatId(long chatId, String link);

    @Query(value = "SELECT 1 FROM rss_list WHERE chat_id = :chatId and lower(source) = lower(:source)", nativeQuery = true)
    Integer isExistsBySourceName(long chatId, String source);

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType = 'rss'")
    List<RssList> findAllActiveRss();

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType <> 'rss'")
    List<RssList> findAllActiveNoRss();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE rss_list SET is_active = :value WHERE link = :link", nativeQuery = true)
    int updateIsActiveRss(int value, String link);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from rss_list where chat_id = :chatId and lower(source) = lower(:source)",
            nativeQuery = true)
    int deleteOwnRss(long chatId, String source);

}
