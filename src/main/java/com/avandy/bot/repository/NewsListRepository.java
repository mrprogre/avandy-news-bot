package com.avandy.bot.repository;

import com.avandy.bot.model.NewsList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.LinkedHashSet;
import java.util.TreeSet;

public interface NewsListRepository extends CrudRepository<NewsList, Long> {

    @Query(value = "SELECT title_hash FROM news_list", nativeQuery = true)
    LinkedHashSet<String> getNewsListAllHash();

    @Query(value = "SELECT id, source, title, title_hash, link, pub_date, add_date FROM news_list" +
            "        WHERE pub_date > (current_timestamp - cast(:period as interval))"
            , nativeQuery = true)
    TreeSet<NewsList> getNewsListByPeriod(String period);

    @Query(value = "SELECT id, source, title, title_hash, link, pub_date, add_date FROM news_list" +
            "        WHERE pub_date > (current_timestamp - cast(:period as interval))" +
            "         AND lower(title) ~ ('^'||:word||'\\s|\\s'||:word||'\\s|\\s'||:word||'$')"
            , nativeQuery = true)
    TreeSet<NewsList> getTopNewsListByPeriodAndWord(String period, String word);

}