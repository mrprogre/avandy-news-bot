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
    private int periodMinutes = 1440;
    public static int totalNewsCounter;
    public static int filteredNewsCounter;
    private final SettingsRepository settingsRepository;
    private final KeywordRepository keywordRepository;
    private final RssRepository rssRepository;
    private final ShowedNewsRepository showedNewsRepository;
    private final ExcludedRepository excludedRepository;
    private final NewsListRepository newsListRepository;
    public static ArrayList<Headline> headlinesTopTen;

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

    public Set<Headline> start(Long chatId, String searchType) {
        boolean isTopSearch = !searchType.equals("all") && !searchType.equals("keywords") && !searchType.equals("top");
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        Set<Headline> headlinesToShow = new TreeSet<>();

        if (searchType.equals("keywords")) {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriod()),
                    () -> periodMinutes = 1440);
        } else if (searchType.equals("top")) {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
                    () -> periodMinutes = 1440);
        } else {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodAll()),
                    () -> periodMinutes = 60);
        }

        if (!isTopSearch) {
            List<Keyword> keywords = keywordRepository.findAllByChatId(chatId);
            List<String> showedNewsHash = showedNewsRepository.findShowedNewsHashByChatId(chatId);
            headlinesTopTen = new ArrayList<>();

            // find news by period
            TreeSet<NewsList> newsListByPeriod = newsListRepository.getNewsListByPeriod(periodMinutes + " minutes");

            for (NewsList news : newsListByPeriod) {
                String rss = news.getSource();
                String title = news.getTitle().trim();
                String hash = Common.getHash(title);
                Date date = news.getPubDate();
                String link = news.getLink();

                int dateDiff = Common.compareDates(new Date(), date, periodMinutes);
                switch (searchType) {

                    /* ALL NEWS SEARCH */
                    case "all" -> {
                        if (title.length() > 15) {

                            if (dateDiff != 0) {
                                if (!showedNewsHash.contains(hash)) {
                                    headlinesToShow.add(new Headline(rss, title, link, date, chatId, 4, hash));
                                }
                            }
                        }
                    }

                    /* KEYWORDS SEARCH */
                    case "keywords" -> {
                        for (Keyword keyword : keywords) {
                            boolean isContains = title.toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                    && title.length() > 15;

                            if (isContains) {
                                if (dateDiff != 0) {
                                    if (!showedNewsHash.contains(hash)) {
                                        headlinesToShow.add(new Headline(rss, title, link, date, chatId, 2, hash));
                                    }
                                }
                            }
                        }
                    }

                    /* TOP */
                    case "top" -> {
                        if (dateDiff != 0) {
                            headlinesTopTen.add(new Headline(rss, title, link, date, chatId, -1, hash));
                        }
                    }
                }
            }
        }

        /* SEARCH BY ONE WORD FROM TOP (this case searchType is word for search)*/
        if (isTopSearch) {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
                    () -> periodMinutes = 1440);
            TreeSet<NewsList> newsListByPeriodAndWord =
                    newsListRepository.getTopNewsListByPeriodAndWord(periodMinutes + " minutes",
                            searchType.replaceAll(TelegramBot.REPLACE_ALL_TOP,""));

            for (NewsList news : newsListByPeriodAndWord) {
                String rss = news.getSource();
                String title = news.getTitle().trim();
                String hash = Common.getHash(title);
                Date date = news.getPubDate();
                String link = news.getLink();

                int dateDiff = Common.compareDates(new Date(), date, periodMinutes);
                if (dateDiff != 0) {
                    headlinesToShow.add(new Headline(rss, title, link, date, chatId, -2, hash));
                }
            }
        }

        totalNewsCounter = headlinesToShow.size();
        // remove titles contains excluded words
        if (searchType.equals("all") && settingsRepository.getExcludedOnOffByChatId(chatId).equals("on")) {
            List<String> allExcludedByChatId = excludedRepository.findExcludedByChatId(chatId);

            for (String word : allExcludedByChatId) {
                headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word.toLowerCase()));
            }
        }
        filteredNewsCounter = headlinesToShow.size();

        if (!isTopSearch) {
            Set<ShowedNews> showedNewsToSave = new HashSet<>();

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
        }

        return headlinesToShow;
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

}