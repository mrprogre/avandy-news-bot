package com.avandy.bot.service;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.Parser;
import com.avandy.bot.utils.ParserJsoup;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
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
    private final ShowedNewsRepository showedNewsRepository;
    private final ExcludedRepository excludedRepository;
    private final NewsListRepository newsListRepository;
    public static Set<Headline> headlinesForTopTen;

    @Autowired
    public Search(SettingsRepository settingsRepository, KeywordRepository keywordRepository,
                  RssRepository rssRepository, ShowedNewsRepository showedNewsRepository,
                  ExcludedRepository excludedRepository, NewsListRepository newsListRepository) {
        this.settingsRepository = settingsRepository;
        this.keywordRepository = keywordRepository;
        this.rssRepository = rssRepository;
        this.showedNewsRepository = showedNewsRepository;
        this.excludedRepository = excludedRepository;
        this.newsListRepository = newsListRepository;
    }

    @Scheduled(cron = "${cron.fill.database}")
    public void scheduler() {
        long start = System.currentTimeMillis();

        int countRome = downloadNewsByRome();
        int countJsoup = downloadNewsByJsoup();

        long searchTime = System.currentTimeMillis() - start;
        if (countRome > 0 && countJsoup > 0) {
            log.info("Сохранено новостей: {} (rome: {} + jsoup: {}) за {} ms", countRome + countJsoup,
                    countRome, countJsoup, searchTime);
        }
    }

    public int downloadNewsByRome() {
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();
        Iterable<RssList> sources = rssRepository.findAllActiveRss();

        try {
            for (RssList source : sources) {
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
            }
            newsListRepository.saveAll(newsList);
        } catch (FeedException f) {
            if (f.getMessage().contains("Invalid XML")) {
                log.info("downloadNewsByRome FeedException: " + f.getMessage());
            } else {
                log.warn("downloadNewsByRome FeedException: " + f.getMessage());
            }
        } catch (Exception e) {
            log.error("downloadNewsByRome Exception: " + e.getMessage());
        }
        return newsList.size();
    }

    public int downloadNewsByJsoup() {
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();
        Iterable<RssList> sources = rssRepository.findAllActiveNoRss();

        try {

            for (RssList source : sources) {
                for (Message message : new ParserJsoup().parse(source.getLink())) {
                    String sourceRss = source.getSource();
                    String title = message.getTitle().trim();
                    String titleHash = Common.getHash(title);
                    Date pubDate = message.getPubDate();
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
            }
            newsListRepository.saveAll(newsList);
        } catch (Exception e) {
            log.error(e.getMessage() + "\n downloadNewsBy Jsoup");
        }
        return newsList.size();
    }

    public Set<Headline> start(Long chatId, String searchType) {
        List<Keyword> keywords = keywordRepository.findAllByChatId(chatId);
        List<String> allExcludedByChatId = excludedRepository.findExcludedByChatId(chatId);
        List<String> showedNewsHash = showedNewsRepository.findShowedNewsHashByChatId(chatId);
        Set<ShowedNews> showedNewsToSave = new HashSet<>();
        Set<Headline> headlinesToShow = new TreeSet<>();
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        headlinesForTopTen = new TreeSet<>();

        if (searchType.equals("keywords")) {
            settings.ifPresentOrElse(value -> periodInMinutes = Common.timeMapper(value.getPeriod()),
                    () -> periodInMinutes = 1440);
        } else {
            settings.ifPresentOrElse(value -> periodInMinutes = Common.timeMapper(value.getPeriodAll()),
                    () -> periodInMinutes = 60);
        }

        // find news by period
        TreeSet<NewsList> newsListByPeriod = newsListRepository.getNewsListByPeriod(periodInMinutes + " minutes");

        for (NewsList news : newsListByPeriod) {
            String sourceRss = news.getSource();
            String title = news.getTitle().trim();
            String titleHash = Common.getHash(title);
            Date pubDate = news.getPubDate();
            String link = news.getLink();

            /* ALL NEWS SEARCH */
            switch (searchType) {
                case "all" -> {
                    if (title.length() > 15) {
                        int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);
                        if (dateDiff != 0) {
                            Headline row = new Headline(sourceRss, title, link, pubDate, chatId, 4, titleHash);

                            if (!showedNewsHash.contains(titleHash)) {
                                headlinesToShow.add(row);
                            }
                        }
                    }
                }
                /* KEYWORDS SEARCH */
                case "keywords" -> {
                    for (Keyword keyword : keywords) {
                        if (title.toLowerCase().contains(keyword.getKeyword().toLowerCase()) && title.length() > 15) {
                            int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);

                            if (dateDiff != 0) {
                                Headline row = new Headline(sourceRss, title, link, pubDate, chatId, 2, titleHash);

                                if (!showedNewsHash.contains(titleHash)) {
                                    headlinesToShow.add(row);
                                }
                            }
                        }
                    }
                }
                case "top" -> {
                    int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);
                    if (dateDiff != 0) {
                        Headline row = new Headline(sourceRss, title, link, pubDate, chatId, 4, titleHash);
                        headlinesForTopTen.add(row);
                    }
                }
            }
        }

        totalNewsCounter = headlinesToShow.size();
        // remove titles contains excluded words
        if (searchType.equals("all") && settingsRepository.getExcludedOnOffByChatId(chatId).equals("on")) {
            for (String word : allExcludedByChatId) {
                headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word.toLowerCase()));
            }
        }
        filteredNewsCounter = headlinesToShow.size();

        ShowedNews showedNewsRow;
        for (Headline line : headlinesToShow) {
            showedNewsRow = new ShowedNews();
            showedNewsRow.setChatId(line.getChatId());
            showedNewsRow.setType(line.getType());
            showedNewsRow.setTitleHash(line.getTitleHash());
            showedNewsToSave.add(showedNewsRow);
        }

        if (showedNewsToSave.size() > 0) {
            showedNewsRepository.saveAll(showedNewsToSave);
        }

        return headlinesToShow;
    }

}