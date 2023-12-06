package com.avandy.bot.repository;

import com.avandy.bot.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {

    @Query(value = "FROM users WHERE isActive = 1")
    List<User> findAllActiveUsers();

    @Query(value = "SELECT count(1) FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isUserExistsByChatId(Long chatId);

}