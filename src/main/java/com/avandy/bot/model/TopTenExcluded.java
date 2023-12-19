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
@Entity(name = "top_ten_excluded")
public class TopTenExcluded {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String word;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopTenExcluded that = (TopTenExcluded) o;
        return Objects.equals(userId, that.userId) && Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, word);
    }
}