package com.avandy.bot.search;

import com.avandy.bot.model.Headline;

import java.util.LinkedList;

public interface SearchService {
    LinkedList<Headline> start(Long chatId, String searchType);

    int downloadNewsByRome();

    int downloadNewsByJsoup();
}
