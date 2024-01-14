package com.avandy.bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InlineKeyboards {

    private static void setButtons(Map<String, String> buttons, List<InlineKeyboardButton> rowInLine) {
        try {
            for (Map.Entry<String, String> item : buttons.entrySet()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(item.getValue());
                button.setCallbackData(item.getKey());
                rowInLine.add(button);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    public static InlineKeyboardMarkup inlineKeyboardMaker(Map<String, String> buttons) {
        InlineKeyboardMarkup inlineKeyboardAbout = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        //создаем кнопки
        setButtons(buttons, rowInLine);
        rowsInLine.add(rowInLine);
        inlineKeyboardAbout.setKeyboard(rowsInLine);

        return inlineKeyboardAbout;
    }

    public static InlineKeyboardMarkup inlineKeyboardMaker(Map<String, String> buttons1,
                                                           Map<String, String> buttons2,
                                                           Map<String, String> buttons3,
                                                           Map<String, String> buttons4,
                                                           Map<String, String> buttons5) {
        InlineKeyboardMarkup inlineKeyboardAbout = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        // 1 этаж
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        setButtons(buttons1, rowInLine1);
        rowsInLine.add(rowInLine1);

        // 2 этаж
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        setButtons(buttons2, rowInLine2);
        rowsInLine.add(rowInLine2);

        // 3 этаж
        if (buttons3 != null) {
            List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();
            setButtons(buttons3, rowInLine3);
            rowsInLine.add(rowInLine3);
        }

        // 4 этаж
        if (buttons4 != null) {
            List<InlineKeyboardButton> rowInLine4 = new ArrayList<>();
            setButtons(buttons4, rowInLine4);
            rowsInLine.add(rowInLine4);
        }

        // 5 этаж
        if (buttons5 != null) {
            List<InlineKeyboardButton> rowInLine5 = new ArrayList<>();
            setButtons(buttons5, rowInLine5);
            rowsInLine.add(rowInLine5);
        }

        inlineKeyboardAbout.setKeyboard(rowsInLine);
        return inlineKeyboardAbout;
    }

}