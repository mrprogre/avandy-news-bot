package com.avandy.bot.repository;

import com.avandy.bot.model.RssList;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RssRepository extends CrudRepository<RssList, Integer> {
    @Query(value = "select distinct source from rss_list r where is_active = 1 and lower(lang) = lower(:lang) " +
            "and not exists(select 1 from rss_excluding e where lower(r.source) = lower(e.source_name) and e.chat_id = :chatId) " +
            "and r.chat_id is null " +
            "order by source",
            nativeQuery = true)
    List<String> findAllActiveSources(long chatId, String lang);

    @Query(value = "SELECT source FROM rss_list WHERE is_active = 1 and chat_id = :chatId ORDER BY source",
            nativeQuery = true)
    List<String> findPersonalSources(long chatId);

    @Query(value = "select 1 from rss_list where chat_id is null and lower(link) = lower(:link)", nativeQuery = true)
    Integer isExistsByChatId(String link);

    @Query(value = "select 1 from rss_list where chat_id = :chatId and lower(source) = lower(:source)", nativeQuery = true)
    Integer isExistsPersonalRssByName(long chatId, String source);

    @Query(value = "select 1 from rss_list where lower(source) = lower(:source)", nativeQuery = true)
    Integer isExistsCommonRssByName(String source);

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
    @Query(value = "insert into rss_excluding(chat_id, source_name) values (:chatId, :source);",
            nativeQuery = true)
    int deleteCommonRss(long chatId, String source);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "delete from rss_list where chat_id = :chatId and lower(source) = lower(:source)",
            nativeQuery = true)
    int deletePersonalRss(long chatId, String source);

}
