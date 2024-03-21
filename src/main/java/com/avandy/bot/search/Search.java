package com.avandy.bot.search;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.ParserJsoup;
import com.avandy.bot.utils.ParserRome;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
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
    public static ArrayList<Headline> headlinesTopTen;

    @Override
    public LinkedList<Headline> start(Long chatId, String searchType) {
        boolean isAllSearch = searchType.equals("all");
        boolean isKeywordAutoSearch = searchType.startsWith("keywords");
        boolean isTopSearch = searchType.equals("top");
        boolean isSearchByOneWordFromTop = !isAllSearch && !isKeywordAutoSearch && !isTopSearch;
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

        List<String> showedNewsHash = showedNewsRepository.findHashAndType(chatId);

        if (isAllSearch || isTopSearch) {
            headlinesTopTen = new ArrayList<>();

            // find news by period TreeSet<YourClass> treeSet = new TreeSet<>(arrayList);
            TreeSet<NewsList> newsListByPeriod =
                    newsListRepository.getNewsListByPeriod(periodMinutes + " minutes", userLanguage, chatId);
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
                                if (!showedNewsHash.contains(hash + 4)) {
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
        if (isKeywordAutoSearch) {
            List<String> keywords = keywordRepository.findKeywordsByChatId(chatId);
            settings.ifPresentOrElse(value -> periodMinutes = Common.timeMapper(value.getPeriod()),
                    () -> periodMinutes = 600);

            String period = periodMinutes + " minutes";
            if (searchType.contains("24")) {
                period = "24 hours";
            }

            Set<NewsList> newsList;
            for (String keyword : keywords) {
                // Замена окончаний слов на *. Применяется только к русским словам.
                keyword = Common.replaceCharsForRegexp(keyword);

                try {
                    newsList = newsListRepository.getNewsWithRegexp(period, keyword, userLanguage, chatId);
                } catch (Exception e) {
                    log.warn(e.getMessage() + ". Keyword: " + keyword);
                    continue;
                }

                for (NewsList news : newsList) {
                    String rss = news.getSource();
                    String title = news.getTitle().trim();
                    String hash = Common.getHash(title);
                    Date date = news.getPubDate();
                    String link = news.getLink();

                    if (title.length() > 15) {
                        if (!showedNewsHash.contains(hash + 2)) {
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
            String word = searchType.replaceAll(Common.REPLACE_ALL_TOP, "");

            String period = periodMinutes + " minutes";
            TreeSet<NewsList> newsList;
            if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId)) && !word.startsWith("chat")) {
                newsList = newsListRepository.getNewsWithLike(period, word, userLanguage, chatId);
            } else if (word.startsWith("chat")) {
                word = word.substring(4);

                newsList = new TreeSet<>();
                String[] words = word.split(" ");
                for (String keyword : words) {
                    keyword = Common.replaceCharsForRegexp(keyword);
                    newsList.addAll(newsListRepository.getNewsWithRegexp(period, keyword, userLanguage, chatId));
                }

            } else {
                word = Common.replaceCharsForRegexp(word);
                try {
                    newsList = newsListRepository.getNewsWithRegexp(period, word, userLanguage, chatId);
                } catch (Exception e) {
                    log.warn(e.getMessage() + ". Word = " + word);
                    return null;
                }
            }

            for (NewsList news : newsList) {
                String rss = news.getSource();
                String title = news.getTitle().trim();
                String hash = Common.getHash(title);
                Date date = news.getPubDate();
                String link = news.getLink();

                headlinesToShow.add(new Headline(rss, title, link, date, chatId, -2, hash));
            }
        }

        totalNewsCounter = headlinesToShow.size();

        // Удаление заголовков, содержащих слова-исключения
        if (isAllSearch && settingsRepository.getExcludedOnOffByChatId(chatId).equals("on")) {
            List<String> allExcludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);

            for (String word : allExcludedByChatId) {
                headlinesToShow.removeIf(x -> x.getTitle().toLowerCase().contains(word));
            }
        }

        // Оставляем одну из нескольких похожих новостей O(n²)
        headlinesToShow.parallelStream()
                .forEach(h1 -> headlinesToShow
                        .forEach(h2 -> {
                            double compare = Common.jaroWinklerCompare(h1.getTitle(), h2.getTitle());
                            if (compare >= 85 && compare != 100) {
                                //log.warn(h1.getPubDate() + " " + h1.getTitle() + " + " + h2.getPubDate() + " " + h2.getTitle() + " = " + compare);
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
        uniqueJw.forEach(headlinesToShow::remove);

        // Все поиски, кроме поиска новостей по Топу, не дают дублирующих заголовков
        if (!isSearchByOneWordFromTop) {
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

        return new LinkedList<>(headlinesToShow);
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