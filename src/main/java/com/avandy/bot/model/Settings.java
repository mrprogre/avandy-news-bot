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
    @Size(max = 32)
    private String period;
    private String periodAll;
    private LocalTime start;
}
