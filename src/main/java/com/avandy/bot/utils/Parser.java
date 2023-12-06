package com.avandy.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;

@Slf4j
public class Parser {
    LocalTime timeStart;
    Duration searchTime;
    final static int LONG_SEARCH = 2;

    public SyndFeed parseFeed(String url) throws IllegalArgumentException, FeedException, IOException {
        timeStart = LocalTime.now();
        InputStream urlConnection = new URL(url).openConnection().getInputStream();
        XmlReader reader = new XmlReader(urlConnection);

        // подсчёт времени поиска
        searchTime = Duration.between(timeStart, LocalTime.now());

        String urlToConsole = getNameFromUrl(url);

        if (searchTime.getSeconds() > LONG_SEARCH)
            log.warn("long search - " + urlToConsole + " - " + searchTime.getSeconds() + " s.");

        return new SyndFeedInput().build(reader);
    }

    private String getNameFromUrl(String url) {
        String urlToConsole = "empty";
        if (url != null) {
            urlToConsole = url.replaceAll(("https://|http://|www."), "").concat("/");
            urlToConsole = urlToConsole.substring(0, urlToConsole.indexOf("/"));
        }
        return urlToConsole;
    }
}