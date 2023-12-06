package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@Entity(name = "all_news")
public class AllNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private Integer type; // 1-send keywords, 2-show keywords, 3-send-all, 4-show-all
    @Size(max = 10)
    private String titleHash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllNews allNews = (AllNews) o;
        return Objects.equals(chatId, allNews.chatId) && Objects.equals(type, allNews.type)
                && Objects.equals(titleHash, allNews.titleHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, type, titleHash);
    }

}