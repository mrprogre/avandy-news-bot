package com.avandy.bot.repository;

import com.avandy.bot.model.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {

    @Query(value = "select word from category where category = :category", nativeQuery = true)
    String [] getWords(String category);

    @Query(value = "select count(word) from category where category = :category", nativeQuery = true)
    int getWordsCountByCategory(String category);

}
