package com.avandy.bot.repository;

import com.avandy.bot.model.NewsList;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.LinkedHashSet;
import java.util.TreeSet;

public interface NewsListRepository extends CrudRepository<NewsList, Long> {

    @Query(value = "SELECT title_hash FROM news_list", nativeQuery = true)
    LinkedHashSet<String> getNewsListAllHash();

    @Query(value = "SELECT n.id, n.source, n.title, n.title_hash, n.link, n.pub_date, n.add_date FROM news_list n" +
            "         JOIN rss_list r on n.source = r.source and lang = :lang" +
            "        WHERE pub_date > (current_timestamp - cast(:period as interval))"
            , nativeQuery = true)
    TreeSet<NewsList> getNewsListByPeriod(String period, String lang);

    @Query(value = "SELECT n.id, n.source, n.title, n.title_hash, n.link, n.pub_date, n.add_date FROM news_list n" +
            "         JOIN rss_list r on n.source = r.source and lang = :lang" +
            "        WHERE pub_date > (current_timestamp - cast(:period as interval))" +
            "          AND lower(title) ~ ('^'||:word||'(\\s|\\W)|(\\s|\\W)'||:word||'(\\s|\\W)|(\\s|\\W)'||:word||'$')"
            , nativeQuery = true)
    TreeSet<NewsList> getNewsWithRegexp(String period, String word, String lang);

    @Query(value = "SELECT n.id, n.source, n.title, n.title_hash, n.link, n.pub_date, n.add_date FROM news_list n" +
            "         JOIN rss_list r on n.source = r.source and lang = :lang" +
            "        WHERE pub_date > (current_timestamp - cast(:period as interval))" +
            "          AND lower(title) ilike '%'||:word||'%'"
            , nativeQuery = true)
    TreeSet<NewsList> getNewsWithLike(String period, String word, String lang);

    /* Schedulers */
    @Query(value = "SELECT count(*) FROM news_list WHERE pub_date > (current_timestamp - cast(:interval as interval))",
            nativeQuery = true)
    int getNewsListCountBy6Hours(String interval);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM news_list WHERE add_date < (current_timestamp - cast('72 hours' as interval))",
            nativeQuery = true)
    int deleteNews72h();
}