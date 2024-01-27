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
    String getKeywordsPeriod(long chatId);

    @Query(value = "SELECT period_all FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getFullSearchPeriod(long chatId);

    @Query(value = "SELECT period_top FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getTopPeriod(long chatId);

    @Query(value = "SELECT scheduler FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getSchedulerOnOffByChatId(long chatId);

    @Query(value = "SELECT excluded FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getExcludedOnOffByChatId(long chatId);

    @Query(value = "SELECT lang FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getLangByChatId(long chatId);

    @Query(value = "select s.* from settings s join users u on s.chat_id = u.chat_id " +
            "where s.scheduler = 'on' and (u.is_premium = 0 or (u.is_premium = 1 and s.premium_search = 'off'))", nativeQuery = true)
    List<Settings> findAllSchedulerOn();

    @Query(value = "select s.* from settings s join users u on s.chat_id = u.chat_id " +
            "where s.scheduler = 'on' and u.is_premium = 1 and s.premium_search = 'on'", nativeQuery = true)
    List<Settings> findAllSchedulerOnPremium();

    @Query(value = "SELECT jaro_winkler FROM settings WHERE chat_id = :chatId", nativeQuery = true)
    String getJaroWinklerByChatId(long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET period = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updateKeywordsPeriod(String value, long chatId);

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

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET jaro_winkler = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updateJaroWinkler(String value, long chatId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE settings SET premium_search = lower(:value) WHERE chat_id = :chatId", nativeQuery = true)
    void updatePremiumSearch(String value, long chatId);
}
