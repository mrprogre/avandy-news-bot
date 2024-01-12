package com.avandy.bot.utils;

import com.avandy.bot.repository.NewsListRepository;
import com.avandy.bot.repository.ShowedNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Monitoring {
    private final NewsListRepository newsListRepository;
    private final ShowedNewsRepository showedNewsRepository;

    // error: Мониторинг работы загрузчика новостей: раз в 2 часа
    @Scheduled(cron = "${cron.monitoring.no.save.news}")
    void getStatNoSaveNews() {
        int newsListCounts = newsListRepository.getNewsListCountBy6Hours("2 hours");
        if (newsListCounts < 10) {
            log.error("За 6 часов сохранено новостей: {}. Проверить!", newsListCounts);
        }
    }

    // warn: Мониторинг количества загруженных новостей: раз в 6 часов
    @Scheduled(cron = "${cron.monitoring.save.news}")
    void getStatSaveNews() {
        int newsListCounts = newsListRepository.getNewsListCountBy6Hours("6 hours");
        if (newsListCounts > 0) {
            log.warn("За 6 часов сохранено новостей: {} ", newsListCounts);
        }
    }

    // info: Очистка таблицы showed_news раз в сутки в 6 утра
    @Scheduled(cron = "${cron.delete.old.showed.news}")
    void deleteOldShowedNews() {
        int newsListCounts = showedNewsRepository.deleteOldShowedNews();
        if (newsListCounts > 0) {
            log.warn("Очистка таблицы showed_news, удалено: {} новостей!", newsListCounts);
        }
    }
}
