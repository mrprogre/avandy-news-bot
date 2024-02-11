package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "news_list")
public class NewsList implements Comparable<NewsList> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String source;
    private String title;
    private String titleHash;
    private String link;
    private Date pubDate;
    private Date addDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsList newsList = (NewsList) o;
        return Objects.equals(titleHash, newsList.titleHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleHash);
    }

    @Override
    public int compareTo(NewsList n) {
        return n.getAddDate().compareTo(addDate);
    }
}