package com.avandy.bot.repository;

import com.avandy.bot.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {

    @Query(value = "SELECT count(1) FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isUserExistsByChatId(Long chatId);

    @Query(value = "SELECT coalesce(first_name, user_name) as name FROM users WHERE chat_id = :chatId",
            nativeQuery = true)
    String findNameByChatId(Long chatId);

    @Query(value = "SELECT * FROM users WHERE is_active = 1", nativeQuery = true)
    List<User> findAllByIsActive();

    @Query(value = "SELECT is_active FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isActive(Long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE users SET is_active = :value WHERE chat_id = :chatId", nativeQuery = true)
    void updateIsActive(int value, long chatId);

}