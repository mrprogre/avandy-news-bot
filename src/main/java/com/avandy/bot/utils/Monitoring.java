package com.avandy.bot.utils;

import com.avandy.bot.repository.NewsListRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class Monitoring {
    private NewsListRepository newsListRepository;

    @Autowired
    public void setNewsListRepository(NewsListRepository newsListRepository) {
        this.newsListRepository = newsListRepository;
    }


    @Scheduled(cron = "${cron.monitoring.no.save.news}")
    void getStatNoSaveNews() {
        if (newsListRepository.getNewsListCountBy6Hours("2 hours") < 10) {
            log.error("За 2 часа не сохранено ни одной новости! Проверить!");
        }
    }

    @Scheduled(cron = "${cron.monitoring.save.news}")
    void getStatSaveNews() {
        int newsListCountBy6Hours = newsListRepository.getNewsListCountBy6Hours("6 hours");
        if (newsListCountBy6Hours > 0) {
            log.warn("За 6 часов сохранено {} новостей!", newsListCountBy6Hours);
        }
    }
}
