package com.avandy.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

@Data
@AllArgsConstructor
public class Headline implements Comparable<Headline> {
    private String source;
    private String title;
    private String link;
    private Date pubDate;
    private Long chatId;
    private Integer type; // 1-send keywords, 2-show keywords, 3-send-all, 4-show-all
    private String titleHash;

    @Override
    public int compareTo(Headline o) {
        return o.getPubDate().compareTo(pubDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Headline headline = (Headline) o;
        return Objects.equals(source, headline.source) && Objects.equals(title, headline.title)
                && Objects.equals(pubDate, headline.pubDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, title, pubDate);
    }
}
