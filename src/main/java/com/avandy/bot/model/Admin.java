package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "admin")
public class Admin {
    @Id
    private Integer id;
    private String key;
    private String value;
}
