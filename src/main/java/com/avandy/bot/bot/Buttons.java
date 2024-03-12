package com.avandy.bot.bot;

import java.util.LinkedHashMap;
import java.util.Map;

public class Buttons {

    public static Map<String, String> getThemes() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("MESSAGE_THEME_1", "1");
        buttons.put("MESSAGE_THEME_2", "2");
        buttons.put("MESSAGE_THEME_3", "3");
        buttons.put("MESSAGE_THEME_4", "4");
        buttons.put("MESSAGE_THEME_5", "5");
        return buttons;
    }

    public static Map<String, String> getLanguages() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DE_BUTTON", "de");
        buttons.put("FR_BUTTON", "fr");
        buttons.put("ES_BUTTON", "es");
        buttons.put("EN_BUTTON", "en");
        buttons.put("RU_BUTTON", "ru");
        return buttons;
    }

    public static Map<String, String> getTopPeriod() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TOP_INTERVAL_1", "1");
        buttons.put("TOP_INTERVAL_2", "2");
        buttons.put("TOP_INTERVAL_4", "4");
        buttons.put("TOP_INTERVAL_6", "6");
        buttons.put("TOP_INTERVAL_8", "8");
        buttons.put("TOP_INTERVAL_12", "12");
        buttons.put("TOP_INTERVAL_24", "24");
        return buttons;
    }

    public static Map<String, String> getFullSearchPeriod() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1_ALL", "1");
        buttons.put("BUTTON_2_ALL", "2");
        buttons.put("BUTTON_4_ALL", "4");
        buttons.put("BUTTON_6_ALL", "6");
        buttons.put("BUTTON_8_ALL", "8");
        buttons.put("BUTTON_12_ALL", "12");
        buttons.put("BUTTON_24_ALL", "24");
        return buttons;
    }

    public static Map<String, String> getChangePeriodKeywords() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1", "1");
        buttons.put("BUTTON_2", "2");
        buttons.put("BUTTON_4", "4");
        buttons.put("BUTTON_6", "6");
        buttons.put("BUTTON_8", "8");
        buttons.put("BUTTON_12", "12");
        buttons.put("BUTTON_24", "24");
        return buttons;
    }

    public static Map<String, String> getAutoSearchOnOff() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");
        return buttons;
    }

    public static Map<String, String> getOnOffPremiumSearch() {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("PREMIUM_SEARCH_OFF", "Off");
        buttons.put("PREMIUM_SEARCH_ON", "On");
        return buttons;
    }

}
