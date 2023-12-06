package com.avandy.bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity(name = "users")
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String LastName;
    private String userName;
    private Timestamp registeredAt;
    private Integer isActive;
}
