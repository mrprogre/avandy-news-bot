package com.avandy.bot.repository;

import com.avandy.bot.model.RssList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RssRepository extends CrudRepository<RssList, Integer> {

    @Query(value = "FROM rss_list WHERE isActive = 1 order by source")
    List<RssList> findAllActiveSources();

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType = 'rss'")
    List<RssList> findAllActiveRss();

    @Query(value = "FROM rss_list WHERE isActive = 1 and parserType <> 'rss'")
    List<RssList> findAllActiveNoRss();

}
