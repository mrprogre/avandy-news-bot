package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity(name = "settings")
public class Settings {
    @Id
    private Long chatId;
    @Size(max = 3)
    private String scheduler;
    @Size(max = 8)
    private String period;
    @Size(max = 8)
    private String periodAll;
    private LocalTime start;
    private String excluded;
    @Size(max = 2)
    private String lang;
    @Size(max = 8)
    private String periodTop;
    @Size(max = 3)
    private String jaroWinkler;
    @Size(max = 3)
    private String premiumSearch;
}
