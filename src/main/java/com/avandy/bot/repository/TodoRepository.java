package com.avandy.bot.repository;

import com.avandy.bot.model.Todo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TodoRepository extends CrudRepository<Todo, Integer> {
    List<Todo> findByChatId(Long chatId);
}