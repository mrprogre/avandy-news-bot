package com.avandy.bot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserState {
    private String state;

    public UserState(String state) {
        this.state = state;
    }
}
