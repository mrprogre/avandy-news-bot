package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Entity(name = "rss_list")
@NoArgsConstructor
public class RssList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long chatId;
    private String source;
    private String link;
    private Integer isActive;
    private Integer position;
    private Timestamp addDate;
    @Size(max = 128)
    private String country;
    @Size(max = 8)
    private String parserType;
    @Size(max = 3)
    private String lang;

    public RssList(Long chatId,String country,  String source, String link, Integer isActive, Integer position,
                   Timestamp addDate, String parserType, String lang) {
        this.chatId = chatId;
        this.country = country;
        this.source = source;
        this.link = link;
        this.isActive = isActive;
        this.position = position;
        this.addDate = addDate;
        this.parserType = parserType;
        this.lang = lang;
    }
}
