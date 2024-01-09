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


    @Scheduled(cron = "${cron.monitoring.save.news}")
    void getStat() {
        if (newsListRepository.getNewsListCountBy6Hours() < 10) {
            log.error("За 2 часа не сохранено ни одной новости! Проверить!");
        }
    }
}
