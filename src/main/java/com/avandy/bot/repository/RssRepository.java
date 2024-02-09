package com.avandy.bot.repository;

import com.avandy.bot.model.RssList;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RssRepository extends CrudRepository<RssList, Integer> {
    @Query(value = "select distinct source from rss_list where is_active = 1 " +
            "and (lang = :lang or chat_id = :chatId) order by source", nativeQuery = true)
    List<String> findAllActiveSources(String lang, long chatId);

    @Query(value = "SELECT source FROM rss_list WHERE is_active = 1 and chat_id = :chatId ORDER BY source",
            nativeQuery = true)
    List<String> findPersonalSources(long chatId);

    @Query(value = "select 1 from rss_list where chat_id = :chatId and link = :link", nativeQuery = true)
    Integer isExistsByChatId(long chatId, String link);

    @Query(value = "select 1 from rss_list where chat_id = :chatId and lower(source) = lower(:source)", nativeQuery = true)
    Integer isExistsBySourceName(long chatId, String source);

    @Query(value = "select * from rss_list where is_active = 1 and parser_type = 'rss'", nativeQuery = true)
    List<RssList> findAllActiveRss();

    @Query(value = "select * from rss_list where is_active = 1 and parser_type <> 'rss'", nativeQuery = true)
    List<RssList> findAllActiveNoRss();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update rss_list set is_active = :value where link = :link", nativeQuery = true)
    int updateIsActiveRss(int value, String link);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from rss_list where chat_id = :chatId and lower(source) = lower(:source)",
            nativeQuery = true)
    int deleteOwnRss(long chatId, String source);

}
