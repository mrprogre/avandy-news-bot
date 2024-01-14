package com.avandy.bot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserState {
    private States state;

    public UserState(States state) {
        this.state = state;
    }
}
