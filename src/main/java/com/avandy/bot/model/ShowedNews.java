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
@Entity(name = "showed_news")
public class ShowedNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private Integer type; // Тип поиска: 2 - поиск по ключевым словам, 4 - полный поиск
    @Size(max = 10)
    private String titleHash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowedNews showedNews = (ShowedNews) o;
        return Objects.equals(chatId, showedNews.chatId) && Objects.equals(type, showedNews.type)
                && Objects.equals(titleHash, showedNews.titleHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, type, titleHash);
    }

}