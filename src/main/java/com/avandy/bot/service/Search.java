package com.avandy.bot.service;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.JaroWinklerDistance;
import com.avandy.bot.utils.Parser;
import com.avandy.bot.utils.ParserJsoup;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class Search {
    private int periodMinutes = 1440;
    private String userLanguage;
    public static int totalNewsCounter;
    public static int filteredNewsCounter;
    private final SettingsRepository settingsRepository;
    private final KeywordRepository keywordRepository;
    private final RssRepository rssRepository;
    private final ShowedNewsRepository showedNewsRepository;
    private final ExcludingTermsRepository excludingTermsRepository;
    private final NewsListRepository newsListRepository;
    public static ArrayList<Headline> headlinesTopTen;
    private final JaroWinklerDistance jwd = new JaroWinklerDistance();

    @Autowired
    public Search(SettingsRepository settingsRepository, KeywordRepository keywordRepository,
                  RssRepository rssRepository, ShowedNewsRepository showedNewsRepository,
                  ExcludingTermsRepository excludingTermsRepository, NewsListRepository newsListRepository) {
        this.settingsRepository = settingsRepository;
        this.keywordRepository = keywordRepository;
        this.rssRepository = rssRepository;
        this.showedNewsRepository = showedNewsRepository;
        this.excludingTermsRepository = excludingTermsRepository;
        this.newsListRepository = newsListRepository;
    }

    public Set<Headline> start(Long chatId, String searchType) {
        boolean isAllSearch = searchType.equals("all");
        boolean isKeywordSearch = searchType.equals("keywords");
        boolean isTopSearch = searchType.equals("top");
        boolean isSearchByOneWordFromTop = !isAllSearch && !isKeywordSearch && !isTopSearch;
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        settings.ifPresentOrElse(value -> userLanguage = value.getLang(), () -> userLanguage = "en");
        Set<Headline> headlinesToShow = new TreeSet<>();
        Set<Headline> headlinesDeleteJw = new HashSet<>();


        if (isTopSearch) {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
                    () -> periodMinutes = 1440);
        } else {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodAll()),
                    () -> periodMinutes = 60);
        }

        List<String> showedNewsHash = showedNewsRepository.findShowedNewsHashByChatId(chatId);
        if (isAllSearch || isTopSearch) {
            headlinesTopTen = new ArrayList<>();

            // find news by period
            TreeSet<NewsList> newsListByPeriod =
                    newsListRepository.getNewsListByPeriod(periodMinutes + " minutes", userLanguage);
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
                                    //findSimilarNews(headlinesToShow, headlinesForDeleteFromShowJW, title);
                                    headlinesToShow.add(new Headline(rss, title, link, date, chatId, 4, hash));
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

        /* KEYWORDS SEARCH */
        if (isKeywordSearch) {
            List<String> keywords = keywordRepository.findKeywordsByChatId(chatId);
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriod()),
                    () -> periodMinutes = 1440);
            String period = periodMinutes + " minutes";

            Set<NewsList> newsList;
            for (String keyword : keywords) {
                // замена * на любой текстовый символ, который может быть или не быть
                if (keyword.contains("*")) keyword = keyword.replace("*", "\\w?");

                //newsList = newsListRepository.getNewsWithLike(period, keyword, userLanguage);
                newsList = newsListRepository.getNewsWithRegexp(period, keyword, userLanguage);

                for (NewsList news : newsList) {
                    String rss = news.getSource();
                    String title = news.getTitle().trim();
                    String hash = Common.getHash(title);
                    Date date = news.getPubDate();
                    String link = news.getLink();

                    if (title.length() > 15) {
                        if (!showedNewsHash.contains(hash)) {
                            // Filtering out similar news. Only for keyword search (O=n*n)
                            //findSimilarNews(headlinesToShow, headlinesForDeleteFromShowJW, title);

                            headlinesToShow.add(new Headline(rss, title, link, date, chatId, 2, hash));
                        }
                    }
                }
            }
        }

        /* SEARCH BY ONE WORD FROM TOP (this case searchType is word for search)*/
        if (isSearchByOneWordFromTop) {
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
                    () -> periodMinutes = 1440);

            String type = searchType.replaceAll(TelegramBot.REPLACE_ALL_TOP, "");
            String period = periodMinutes + " minutes";
            TreeSet<NewsList> newsList;
            if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId))) {
                newsList = newsListRepository.getNewsWithLike(period, type, userLanguage);
            } else {
                newsList = newsListRepository.getNewsWithRegexp(period, type, userLanguage);
            }

            for (NewsList news : newsList) {
                String rss = news.getSource();
                String title = news.getTitle().trim();
                String hash = Common.getHash(title);
                Date date = news.getPubDate();
                String link = news.getLink();

                //findSimilarNews(headlinesToShow, headlinesForDeleteFromShowJW, title);
                headlinesToShow.add(new Headline(rss, title, link, date, chatId, -2, hash));
            }
        }

        totalNewsCounter = headlinesToShow.size();
        // remove titles contains excluding terms
        if (isAllSearch && settingsRepository.getExcludedOnOffByChatId(chatId).equals("on")) {
            List<String> allExcludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);

            for (String word : allExcludedByChatId) {
                headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word.toLowerCase()));
            }
        }

        // Filtering out similar news: O(n²)
        headlinesToShow.parallelStream().forEach(headline1 -> headlinesToShow.parallelStream().forEach(headline2 -> {
            int compare = jwd.compare(headline1.getTitle(), headline2.getTitle());
            boolean isSource = headline1.getSource().equals(headline2.getSource());

            if (compare >= 85 && ((compare != 100 && isSource)) || (compare == 100 && !isSource)) {
                headlinesDeleteJw.add(headline1);
                headlinesDeleteJw.remove(headline2);
            }
        }));

        // Delete similar news
        for (Headline headline : headlinesDeleteJw) {
            log.warn("Удалена дублирующая новость. " + chatId + ": " + ", source: " + headline.getSource() + ", " + headline.getTitle());
        }
        headlinesToShow.removeAll(headlinesDeleteJw);

        filteredNewsCounter = headlinesToShow.size();

        if (!isSearchByOneWordFromTop) {
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
        if (countRome > 0 || countJsoup > 0) {
            log.info("Сохранено новостей: {} (rome: {} + jsoup: {}) за {} ms", countRome + countJsoup,
                    countRome, countJsoup, searchTime);
        }
    }

    public int downloadNewsByRome() {
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();
        List<RssList> sources = rssRepository.findAllActiveRss();

        try {
            for (RssList source : sources) {
                List<SyndEntry> entries;
                try {
                    entries = new Parser().parseFeed(source.getLink()).getEntries();
                } catch (Exception e) {
                    if (e.getMessage().contains("Invalid XML")) {
                        log.info("Invalid XML: " + source.getSource());
                    } else {
                        log.warn("Error: download news by Rome, source: {}, {}", source.getSource(), e.getMessage());
                    }
                    continue;
                }

                for (SyndEntry message : entries) {
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
                List<Message> entries;
                try {
                    entries = new ParserJsoup().parse(source.getLink());
                } catch (Exception e) {
                    if (e.getMessage().contains("Invalid XML")) {
                        log.info("Invalid XML: " + source.getSource());
                    } else {
                        log.error("Error: download news by Jsoup, source: {}, {}", source.getSource(), e.getMessage());
                    }
                    continue;
                }

                for (Message message : entries) {
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