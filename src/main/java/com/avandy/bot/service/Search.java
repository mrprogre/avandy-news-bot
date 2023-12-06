package com.avandy.bot.service;

import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.Parser;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    @Autowired
    public Search(SettingsRepository settingsRepository, KeywordRepository keywordRepository,
                  RssRepository rssRepository, AllNewsRepository allNewsRepository,
                  ExcludedRepository excludedRepository) {
        this.settingsRepository = settingsRepository;
        this.keywordRepository = keywordRepository;
        this.rssRepository = rssRepository;
        this.allNewsRepository = allNewsRepository;
        this.excludedRepository = excludedRepository;
    }

    public Set<Headline> start(Long chatId, String mode, String searchType) {
        Optional<Settings> settings = settingsRepository.findById(chatId).stream().findFirst();
        List<Keyword> keywords = keywordRepository.findAllByChatId(chatId);
        List<String> allExcludedByChatId = excludedRepository.findExcludedByChatId(chatId);
        List<String> allNewsHash = allNewsRepository.findAllNewsHashByChatId(chatId);
        Set<AllNews> allNewsToSave = ConcurrentHashMap.newKeySet();
        SortedSet<Headline> headlinesToShow = Collections.synchronizedSortedSet(new TreeSet<>());

        try {
            Iterable<RssList> sources = rssRepository.findAllActiveRss();

            for (RssList source : sources) {
                try {
                    for (SyndEntry message : new Parser().parseFeed(source.getLink()).getEntries()) {
                        String sourceRss = source.getSource();
                        String title = message.getTitle();
                        Date pubDate = message.getPublishedDate();
                        String link = message.getLink();

                        /* ALL NEWS SEARCH */
                        if (searchType.equals("all")) {
                            if (title.length() > 15) {
                                settings.ifPresent(value -> periodInMinutes = Common.timeMapper(value.getPeriodAll()));

                                int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);
                                if (dateDiff != 0) {
                                    if (mode.equals("show-all")) {
                                        String titleHash = Common.getHash(title);
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
                            for (Keyword keyword : keywords) {
                                settings.ifPresent(value -> periodInMinutes = Common.timeMapper(value.getPeriod()));

                                if (title.toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                        && title.length() > 15) {
                                    int dateDiff = Common.compareDates(new Date(), pubDate, periodInMinutes);

                                    if (dateDiff != 0) {
                                        if (mode.equals("show")) {
                                            String titleHash = Common.getHash(title);
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

            if (allNewsToSave.size() > 0 && !TelegramBot.test) {
                allNewsRepository.saveAll(allNewsToSave);
            }

        } catch (Exception e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
        return headlinesToShow;
    }

}