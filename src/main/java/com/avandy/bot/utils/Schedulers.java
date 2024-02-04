package com.avandy.bot.utils;

import com.avandy.bot.bot.TelegramBot;
import com.avandy.bot.model.Settings;
import com.avandy.bot.repository.*;
import com.avandy.bot.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Schedulers {
    private final SearchService searchService;
    private final NewsListRepository newsListRepository;
    private final ShowedNewsRepository showedNewsRepository;
    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final TelegramBot telegramBot;
    private final AdminRepository adminRepository;
    private final CommonQuery commonQuery;


    // Поиск новостей каждый час
    @Scheduled(cron = "${cron.search.keywords}")
    protected void autoSearchByKeywords() {
        searchNewsByKeywords(settingsRepository.findAllSchedulerOn());
    }

    // Премиум пользователи: поиск новостей каждые 2 минуты
    @Scheduled(cron = "${cron.search.keywords.premium}")
    protected void autoSearchByKeywordsPremium() {
        searchNewsByKeywords(settingsRepository.findAllSchedulerOnPremium());

    }

    private void searchNewsByKeywords(List<Settings> usersSettings) {
        Integer hourNow = LocalTime.now().getHour();

        for (Settings setting : usersSettings) {
            List<Integer> timeToExecute = Common.getTimeToExecute(setting.getStart(), setting.getPeriod());

            if (timeToExecute.contains(hourNow)) {
                Long chatId = setting.getChatId();

                if (userRepository.isActive(chatId) == 1) {
                    telegramBot.autoSearchNewsByKeywords(chatId);
                }
            }
        }
    }

    // Сохранение всех новостей в БД по всем источникам каждые 2 минуты
    @Scheduled(cron = "${cron.fill.database}")
    public void scheduler() {
        long start = System.currentTimeMillis();

        int countRome = searchService.downloadNewsByRome();
        int countJsoup = searchService.downloadNewsByJsoup();

        long searchTime = System.currentTimeMillis() - start;
        if ((countRome > 0 || countJsoup > 0) && searchTime > 50000L) {
            log.warn("Сохранено новостей: {} (rome: {} + jsoup: {}) за {} s", countRome + countJsoup,
                    countRome, countJsoup, searchTime / 1000);
        }
    }

    // error: Мониторинг работы загрузчика новостей: раз в 2 часа
    @Scheduled(cron = "${cron.monitoring.no.save.news}")
    void getStatNoSaveNews() {
        int newsListCounts = newsListRepository.getNewsListCountBy6Hours("2 hours");
        if (newsListCounts < 5) {
            log.error("Загрузчик новостей не работает, сохранено новостей: {}!", newsListCounts);
        }
    }

    // info: Мониторинг количества загруженных новостей: раз в 6 часов
    @Scheduled(cron = "${cron.monitoring.save.news}")
    void getStatSaveNews() {
        int newsListCounts = newsListRepository.getNewsListCountBy6Hours("6 hours");
        if (newsListCounts > 0) {
            log.warn("За 6 часов сохранено новостей: {} ", newsListCounts);
        }
    }

    // info: Очистка таблицы news_list раз в сутки в 4 утра
    @Scheduled(cron = "${cron.delete.old.news}")
    void deleteOldNewsList() {
        if (adminRepository.findValueByKey("news_list_clear_scheduler").equals("on")) {
            int newsListCounts = newsListRepository.deleteNews72h();
            if (newsListCounts > 0) {
                log.warn("Очистка таблицы news_list, удалено записей: {}", newsListCounts);
            }
        }
    }

    // info: Очистка таблицы showed_news раз в сутки в 6 утра
    @Scheduled(cron = "${cron.delete.old.showed.news}")
    void deleteOldShowedNews() {
        if (adminRepository.findValueByKey("showed_news_clear_scheduler").equals("on")) {
            int newsListCounts = showedNewsRepository.deleteOldNews();
            if (newsListCounts > 0) {
                log.warn("Очистка таблицы showed_news, удалено записей: {}", newsListCounts);
            }
        }
    }

    // info: Количество активных пользователей: раз в сутки в 8 утра
    @Scheduled(cron = "${cron.active.users.count}")
    void activeUsersCount() {
        int usersCount = userRepository.activeUsersCount();
        if (usersCount > 0) {
            log.warn("Количество активных пользователей: {}", usersCount);
        }
    }

    // Удаление пользователей, заблокировавших бота, каждые 30 минут
    @Scheduled(cron = "${cron.delete.users.blocked}")
    void deleteBlockedUsers() {
        int count = userRepository.deleteBlockedUsers();
        if (count > 0)
            log.warn("# Удалено неактивных пользователей: " + count);
    }

    // Отключение Premium по истечении его срока
    @Scheduled(cron = "${cron.revoke.premium.when.expire}")
    void autoRevokePremiumWhenExpire() {
        int count = userRepository.autoRevokePremiumWhenExpire();
        if (count > 0)
            log.warn("# Деактивировано премиум подписок: " + count);
    }

    // Статистика показанных пользователям новостей за период
    @Scheduled(cron = "${cron.get.stat}")
    void getStat() {
        commonQuery.getShowedNewsByPeriod();
    }

}