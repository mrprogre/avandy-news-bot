package com.avandy.bot.service;

import com.avandy.bot.model.Headline;

import java.util.Set;

public interface SearchService {
    Set<Headline> start(Long chatId, String searchType);

    int downloadNewsByRome();

    int downloadNewsByJsoup();
}
