package com.avandy.bot.repository;

import com.avandy.bot.model.TopTenExcluded;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopTenRepository extends JpaRepository<TopTenExcluded, Long> {

    @Query(value = "SELECT lower(word) as word FROM top_ten_excluded ORDER BY id DESC", nativeQuery = true)
    List<String> findAllExcludedFromTopTen();

}