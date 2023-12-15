package com.avandy.bot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class Message {
    private String title;
    private String link;
    private Date pubDate;
}
