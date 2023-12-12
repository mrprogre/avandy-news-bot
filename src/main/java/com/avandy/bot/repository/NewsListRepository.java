package com.avandy.bot.repository;

import com.avandy.bot.model.NewsList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.LinkedHashSet;

public interface NewsListRepository extends CrudRepository<NewsList, Long> {

    @Query(value = "SELECT title_hash FROM news_list", nativeQuery = true)
    LinkedHashSet<String> getNewsListAllHash();
}