package com.avandy.bot.service;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.Parser;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class Search {
    private int periodInMinutes = 1440;
    public static int totalNewsCounter;
    public static int filteredNewsCounter;
    private final SettingsRepository settingsRepository;
    private final KeywordRepository keywordRepository;
    private final RssRepository rssRepository;
    private final AllNewsRepository allNewsRepository;
    private final ExcludedRepository excludedRepository;
    private final NewsListRepository newsListRepository;

    @Autowired
    public Search(SettingsRepository settingsRepository, KeywordRepository keywordRepository,
                  RssRepository rssRepository, AllNewsRepository allNewsRepository,
                  ExcludedRepository excludedRepository, NewsListRepository newsListRepository) {
        this.settingsRepository = settingsRepository;
        this.keywordRepository = keywordRepository;
        this.rssRepository = rssRepository;
        this.allNewsRepository = allNewsRepository;
        this.excludedRepository = excludedRepository;
        this.newsListRepository = newsListRepository;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void downloadNewsByPeriodFromDatabase() {
        long start = System.currentTimeMillis();
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();

        try {
            Iterable<RssList> sources = rssRepository.findAllActiveRss();

            for (RssList source : sources) {
                try {
                    for (SyndEntry message : new Parser().parseFeed(source.getLink()).getEntries()) {
                        String sourceRss = source.getSource();
                        String title = message.getTitle().trim();
                        String titleHash = Common.getHash(title);
                        Date pubDate = message.getPublishedDate();
                        String link = message.getLink();

                        if (!newsListAllHash.contains(titleHash)) {
                            newsList.add(NewsList.builder()
                                    .source(sourceRss)
                                    .title(title)
                                    .titleHash(titleHash)
                                    .link(link)
                                    .pubDate(pubDate)
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            newsListRepository.saveAll(newsList);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        long searchTime = System.currentTimeMillis() - start;
        log.warn("Сохранено новостей - {} за {} мс", newsList.size(), searchTime);
    }

    public Set<Headline> start(Long chatId, String mode, String searchType) {
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        List<Keyword> keywords = keywordRepository.findAllByChatId(chatId);
        List<String> allExcludedByChatId = excludedRepository.findExcludedByChatId(chatId);
        List<String> allNewsHash = allNewsRepository.findAllNewsHashByChatId(chatId);
        Set<AllNews> allNewsToSave = new HashSet<>();
        Set<Headline> headlinesToShow = new TreeSet<>();
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();

        try {
            Iterable<RssList> sources = rssRepository.findAllActiveRss();

            for (RssList source : sources) {
                try {
                    for (SyndEntry message : new Parser().parseFeed(source.getLink()).getEntries()) {
                        String sourceRss = source.getSource();
                        String title = message.getTitle().trim();
                        String titleHash = Common.getHash(title);
                        Date pubDate = message.getPublishedDate();
                        String link = message.getLink();

                        // Save all headlines
                        if (!newsListAllHash.contains(titleHash)) {
                            newsList.add(NewsList.builder()
                                    .source(sourceRss)
                                    .title(title)
                                    .titleHash(titleHash)
                                    .link(link)
                                    .pubDate(pubDate)
                                    .build());
                        }

                        /* ALL NEWS SEARCH */
                        if (searchType.equals("all")) {
                            if (title.length() > 15) {
                                settings.ifPresent(value -> periodInMinutes = Common.timeMapper(value.getPeriodAll()));

                                int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);
                                if (dateDiff != 0) {
                                    if (mode.equals("show-all")) {
                                        Headline headlineRow = new Headline(sourceRss, title, link, pubDate,
                                                chatId, 4, titleHash);

                                        if (!allNewsHash.contains(titleHash)) {
                                            headlinesToShow.add(headlineRow);
                                        }
                                    }
                                }
                            }

                            /* KEYWORDS SEARCH */
                        } else if (searchType.equals("keywords")) {
                            settings.ifPresent(value -> periodInMinutes = Common.timeMapper(value.getPeriod()));

                            for (Keyword keyword : keywords) {
                                if (title.toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                        && title.length() > 15) {
                                    int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);

                                    if (dateDiff != 0) {
                                        if (mode.equals("show")) {
                                            Headline headlineRow = new Headline(sourceRss, title, link, pubDate,
                                                    chatId, 2, titleHash);

                                            if (!allNewsHash.contains(titleHash)) {
                                                headlinesToShow.add(headlineRow);
                                            }
                                        }
                                    }

                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

            newsListRepository.saveAll(newsList);

            totalNewsCounter = headlinesToShow.size();

            // remove titles contains excluded words
            if (mode.equals("show-all")) {
                for (String word : allExcludedByChatId) {
                    headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word.toLowerCase()));
                }
            }

            filteredNewsCounter = headlinesToShow.size();

            AllNews allNewsRow;
            for (Headline line : headlinesToShow) {
                allNewsRow = new AllNews();
                allNewsRow.setChatId(line.getChatId());
                allNewsRow.setType(line.getType());
                allNewsRow.setTitleHash(line.getTitleHash());
                allNewsToSave.add(allNewsRow);
            }

            if (allNewsToSave.size() > 0) {
                allNewsRepository.saveAll(allNewsToSave);
            }

        } catch (Exception e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
        return headlinesToShow;
    }

}