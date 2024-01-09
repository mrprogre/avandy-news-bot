package com.avandy.bot.repository;

import com.avandy.bot.model.RssList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RssRepository extends CrudRepository<RssList, Integer> {
    @Query(value = "SELECT * FROM rss_list WHERE is_active = 1 AND lang = :lang ORDER BY source", nativeQuery = true)
    List<RssList> findAllActiveSources(String lang);

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType = 'rss'")
    List<RssList> findAllActiveRss();

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType <> 'rss'")
    List<RssList> findAllActiveNoRss();
}
