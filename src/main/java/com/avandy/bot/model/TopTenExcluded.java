package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity(name = "top_ten_excluded")
public class TopTenExcluded {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String word;

    public TopTenExcluded(Long userId, String word) {
        this.userId = userId;
        this.word = word;
    }
}