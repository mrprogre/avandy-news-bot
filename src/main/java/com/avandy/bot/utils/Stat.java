package com.avandy.bot.utils;

import com.avandy.bot.repository.NewsListRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class Stat {
    private NewsListRepository newsListRepository;

    @Autowired
    public void setNewsListRepository(NewsListRepository newsListRepository) {
        this.newsListRepository = newsListRepository;
    }


    @Scheduled(cron = "${cron.monitoring.save.news}")
    void getStat() {
        int newsListCountBy6Hours = newsListRepository.getNewsListCountBy6Hours();
        if (newsListCountBy6Hours > 0) {
            log.warn("За 6 часов сохранено {} новостей", newsListCountBy6Hours);
        } else {
            log.error("За 6 часов не сохранена ни одна новость! Проверить!");
        }
    }
}
