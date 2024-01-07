package com.avandy.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
public class Parser {

    public SyndFeed parseFeed(String url) throws FeedException {
        XmlReader reader = null;
        try {
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(3000);
            InputStream inputStream = urlConnection.getInputStream();
            reader = new XmlReader(inputStream);
        } catch (Exception e) {
            if (e.getMessage().contains("Connect timed out")) {
                log.warn("Connect timed out, url: {}", url);
            } else if (e.getMessage().contains("Connection reset")) {
                log.warn("Connection reset, url: {}", url);
            } else {
                log.error(e.getMessage());
            }
        }
        return new SyndFeedInput().build(reader);
    }
}