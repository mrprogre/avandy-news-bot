package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@Entity(name = "top_excluded")
public class TopExcluded {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String word;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopExcluded that = (TopExcluded) o;
        return Objects.equals(chatId, that.chatId) && Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, word);
    }
}