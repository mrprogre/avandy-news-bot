package com.avandy.bot.repository;

import com.avandy.bot.model.Settings;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

    @Query(value = "SELECT period FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getPeriodByChatId(long chatId);

    @Query(value = "SELECT period_all FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getPeriodAllByChatId(long chatId);

    @Query(value = "SELECT period_top FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getPeriodTopByChatId(long chatId);

    @Query(value = "SELECT scheduler FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getSchedulerOnOffByChatId(long chatId);

    @Query(value = "SELECT excluded FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getExcludedOnOffByChatId(long chatId);

    @Query(value = "SELECT lang FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getLangByChatId(long chatId);

    @Query(value = "FROM settings WHERE scheduler = 'on'")
    List<Settings> findAllByScheduler();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET period = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updatePeriod(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET period_all = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updatePeriodAll(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET start = :value WHERE chat_id = :chatId", nativeQuery = true)
    void updateStart(LocalTime value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET scheduler = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updateScheduler(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET excluded = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updateExcluded(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET lang = :value WHERE chat_id = :chatId", nativeQuery = true)
    void updateLanguage(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET period_top = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updatePeriodTop(String value, long chatId);
}
