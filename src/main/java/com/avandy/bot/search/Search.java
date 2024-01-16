package com.avandy.bot.search;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.JaroWinklerDistance;
import com.avandy.bot.utils.ParserRome;
import com.avandy.bot.utils.ParserJsoup;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class Search implements SearchService {
    private int periodMinutes = 1440;
    private String userLanguage;
    private final SettingsRepository settingsRepository;
    private final KeywordRepository keywordRepository;
    private final RssRepository rssRepository;
    private final ShowedNewsRepository showedNewsRepository;
    private final ExcludingTermsRepository excludingTermsRepository;
    private final NewsListRepository newsListRepository;
    public static int totalNewsCounter;
    public static int filteredNewsCounter;
    public static int keywordsNewsCounter;
    public static ArrayList<Headline> headlinesTopTen;

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

    @Override
    public Set<Headline> start(Long chatId, String searchType) {
        boolean isAllSearch = searchType.equals("all");
        boolean isKeywordSearch = searchType.equals("keywords");
        boolean isTopSearch = searchType.equals("top");
        //boolean isSearchByOneWordFromTop = !isAllSearch && !isKeywordSearch && !isTopSearch;
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        settings.ifPresentOrElse(value -> userLanguage = value.getLang(), () -> userLanguage = "en");
        Set<ShowedNews> showedNewsToSave = new HashSet<>();
        Set<Headline> headlinesToShow = new TreeSet<>();
        List<Headline> headlinesDeleteJw = new ArrayList<>();

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
                keywordsNewsCounter += newsList.size();

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
//        if (isSearchByOneWordFromTop) {
//            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
//                    () -> periodMinutes = 1440);
//
//            String type = searchType.replaceAll(Common.REPLACE_ALL_TOP, "");
//            String period = periodMinutes + " minutes";
//            TreeSet<NewsList> newsList;
//            if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId))) {
//                newsList = newsListRepository.getNewsWithLike(period, type, userLanguage);
//            } else {
//                newsList = newsListRepository.getNewsWithRegexp(period, type, userLanguage);
//            }
//
//            for (NewsList news : newsList) {
//                String rss = news.getSource();
//                String title = news.getTitle().trim();
//                String hash = Common.getHash(title);
//                Date date = news.getPubDate();
//                String link = news.getLink();
//
//                //findSimilarNews(headlinesToShow, headlinesForDeleteFromShowJW, title);
//                headlinesToShow.add(new Headline(rss, title, link, date, chatId, -2, hash));
//            }
//        }

        totalNewsCounter = headlinesToShow.size();
        // remove titles contains excluding terms
        if (isAllSearch && settingsRepository.getExcludedOnOffByChatId(chatId).equals("on")) {
            List<String> allExcludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);

            for (String word : allExcludedByChatId) {
                headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word.toLowerCase()));
            }
        }

        // Filtering out similar news: O(n²)
        JaroWinklerDistance jwd = new JaroWinklerDistance();
        headlinesToShow.parallelStream()
                .forEach(h1 -> headlinesToShow
                        .forEach(h2 -> {
                            int compare = jwd.compare(h1.getTitle(), h2.getTitle());
                            if (compare >= 85 && compare != 100) {
                                headlinesDeleteJw.add(h1);
                                headlinesDeleteJw.remove(h2);
                                // Не показывать при следующих поисках
                                if (!showedNewsHash.contains(h1.getTitleHash())) {
                                    showedNewsToSave.add(new ShowedNews(h1.getChatId(), h1.getType(), h1.getTitleHash()));
                                }
                            }
                        }));

        // Deleting news of the same meaning
        List<Headline> uniqueJw = headlinesDeleteJw.stream().distinct().toList();
        for (Headline headline : uniqueJw) {
            log.info("Дублирующая новость: " + chatId + ", source: " + headline.getSource() + ", " + headline.getTitle());
        }
        uniqueJw.forEach(headlinesToShow::remove);

        filteredNewsCounter = headlinesToShow.size();

        // Все поиски, кроме поиска новостей по Топу, не дают дублирующих заголовков
        //if (!isSearchByOneWordFromTop) {
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
        //}

        return headlinesToShow;
    }

    @Override
    public Set<Headline> searchByWordFromTop(long chatId, String word) {
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        settings.ifPresentOrElse(value -> userLanguage = value.getLang(), () -> userLanguage = "en");
        Set<Headline> headlinesToShow = new TreeSet<>();

        settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriodTop()),
                () -> periodMinutes = 1440);

        word = word.replaceAll(Common.REPLACE_ALL_TOP, "");
        String period = periodMinutes + " minutes";
        TreeSet<NewsList> newsList;
        if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId))) {
            newsList = newsListRepository.getNewsWithLike(period, word, userLanguage);
        } else {
            newsList = newsListRepository.getNewsWithRegexp(period, word, userLanguage);
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
        return headlinesToShow;
    }

    @Override
    public int downloadNewsByRome() {
        Set<NewsList> newsList = new LinkedHashSet<>();
        LinkedHashSet<String> newsListAllHash = newsListRepository.getNewsListAllHash();
        List<RssList> sources = rssRepository.findAllActiveRss();

        try {
            for (RssList source : sources) {
                List<SyndEntry> entries;
                try {
                    entries = new ParserRome().parseFeed(source.getLink()).getEntries();
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

    @Override
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