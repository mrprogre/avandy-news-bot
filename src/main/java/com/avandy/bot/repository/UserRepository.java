package com.avandy.bot.repository;

import com.avandy.bot.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {

    @Query(value = "SELECT count(1) FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isUserExistsByChatId(Long chatId);

    @Query(value = "SELECT count(1) FROM users WHERE chat_id = :chatId and is_premium = 1 " +
            "and prem_exp_date > current_date", nativeQuery = true)
    int isPremiumByChatId(Long chatId);

    @Query(value = "SELECT coalesce(first_name, user_name) as name FROM users WHERE chat_id = :chatId",
            nativeQuery = true)
    String findNameByChatId(Long chatId);

    @Query(value = "select u.* from users u join settings s on u.chat_id = s.chat_id where u.is_active = 1 " +
            "and s.lang = 'ru'", nativeQuery = true)
    List<User> findAllRusByIsActive();

    @Query(value = "SELECT prem_exp_date FROM users WHERE chat_id = :chatId", nativeQuery = true)
    Date findPremiumExpDateByChatId(Long chatId);

    @Query(value = "SELECT is_active FROM users WHERE chat_id = :chatId", nativeQuery = true)
    int isActive(Long chatId);

    @Query(value = "SELECT count(1) FROM users WHERE is_active = 1", nativeQuery = true)
    int activeUsersCount();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE users SET is_active = :value WHERE chat_id = :chatId", nativeQuery = true)
    void updateIsActive(int value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE users SET is_premium = 0 WHERE prem_exp_date < current_date and is_premium = 1",
            nativeQuery = true)
    int autoRevokePremiumWhenExpire();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM users WHERE is_active = 0", nativeQuery = true)
    int deleteBlockedUsers();

}