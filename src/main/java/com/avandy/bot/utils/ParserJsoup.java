package com.avandy.bot.utils;

import com.avandy.bot.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
public class ParserJsoup {
    private final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

    public List<Message> parse(String url) {
        List<Message> messages = new ArrayList<>();
        Message message;

        try {
            Document document = Jsoup.connect(url)
                    .timeout(2000)
                    .get();

            Elements news = document.getElementsByTag("item");
            for (Element element : news) {
                String title = element.getElementsByTag("title").text();

                String link = element.getElementsByTag("link").text();
                if (link.isBlank()) link = element.ownText();

                String date = element.getElementsByTag("pubDate").text();
                if (!date.isBlank()) date = date.substring(5, 25);
                else continue;
                Date pubDate = dateFormat.parse(date);

                message = new Message();
                message.setTitle(title);
                message.setLink(link);
                message.setPubDate(pubDate);
                messages.add(message);
            }
        } catch (SocketTimeoutException e) {
            log.info("Read timeout: " + url + " " + e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ParseException e) {
            log.error("ParseException: " + e.getMessage());
        }
        return messages;
    }
}