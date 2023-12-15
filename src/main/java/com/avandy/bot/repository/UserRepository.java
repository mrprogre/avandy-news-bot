package com.avandy.bot.repository;

import com.avandy.bot.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {

    @Query(value = "SELECT count(1) FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isUserExistsByChatId(Long chatId);

    @Query(value = "SELECT coalesce(user_name, first_name) as name FROM users WHERE chat_id = :chatId",
            nativeQuery = true)
    String findNameByChatId(Long chatId);

}