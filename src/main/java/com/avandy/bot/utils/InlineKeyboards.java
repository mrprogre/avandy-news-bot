package com.avandy.bot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class InlineKeyboards {

    private static void setButtons(Map<String, String> buttons, List<InlineKeyboardButton> rowInLine) {
        for (Map.Entry<String, String> item : buttons.entrySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(item.getValue());
            button.setCallbackData(item.getKey());
            rowInLine.add(button);
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
                                                           Map<String, String> buttons3) {
        InlineKeyboardMarkup inlineKeyboardAbout = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();

        // создаем первый этаж
        setButtons(buttons1, rowInLine1);
        // создаем второй этаж
        setButtons(buttons2, rowInLine2);
        // создаем третий этаж
        setButtons(buttons3, rowInLine3);

        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);
        rowsInLine.add(rowInLine3);
        inlineKeyboardAbout.setKeyboard(rowsInLine);
        return inlineKeyboardAbout;
    }

}