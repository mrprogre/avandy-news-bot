package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Objects;

@Data
@Entity(name = "excluded")
public class Excluded {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String word;
    private Long chatId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Excluded excluded = (Excluded) o;
        return Objects.equals(word, excluded.word) && Objects.equals(chatId, excluded.chatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, chatId);
    }
}


