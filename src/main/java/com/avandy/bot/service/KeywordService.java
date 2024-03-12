package com.avandy.bot.service;

import com.avandy.bot.model.Keyword;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static com.avandy.bot.bot.Text.*;

@Slf4j
@Service
public class KeywordService {
    @Value("${bot.owner}")
    public long OWNER_ID;
    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final SettingsRepository settingsRepository;

    public KeywordService(UserRepository userRepository, KeywordRepository keywordRepository,
                          SettingsRepository settingsRepository) {
        this.userRepository = userRepository;
        this.keywordRepository = keywordRepository;
        this.settingsRepository = settingsRepository;
    }


    // Добавление ключевых слов для поиска
    public List<String> addKeywords(long chatId, Set<String> keywords) {
        int counter = 0;
        List<String> messages = new ArrayList<>();
        int isPremium = userRepository.isPremiumByChatId(chatId);
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);
        int currentKeywordsCount = keywordRepository.getKeywordsCountByChatId(chatId);
        int totalKeywordsCount = currentKeywordsCount + keywords.size();

        if (isPremium != 1 && totalKeywordsCount > Common.MAX_KEYWORDS_COUNT && chatId != OWNER_ID) {
            messages.add(premiumIsActive3);
            return messages;
        }

        if (totalKeywordsCount > Common.MAX_KEYWORDS_COUNT_PREMIUM) {
            messages.add(premiumIsActive4);
            return messages;
        }

        Keyword word;
        for (String keyword : keywords) {
            keyword = keyword.replace("\"", "")
                    .trim()
                    .toLowerCase();

            if (keyword.length() >= 2 && keyword.length() <= 64) {
                if (!keywordsByChatId.contains(keyword)) {
                    word = new Keyword();
                    word.setChatId(chatId);
                    word.setKeyword(keyword);

                    try {
                        keywordRepository.save(word);
                        counter++;
                    } catch (Exception e) {
                        if (e.getMessage().contains("ui_keywords_chat_id_link")) {
                            log.info(wordIsExistsText + keyword);
                        }
                    }

                } else {
                    messages.add(wordIsExistsText + keyword);
                }
            } else {
                messages.add(minWordLengthText);
            }
        }

        if (counter != 0) {
            messages.add(wordsAddedText + " - " + counter + " ✔️");
            messages.add(Common.DONE);
        } else {
            messages.add(wordsIsNotAddedText);
        }
        return messages;
    }

    // Формирование списка ключевых слов в виде строки
    public String getKeywordsList(long chatId) {
        int counter = 0;
        StringJoiner joinerKeywords = new StringJoiner("\n");
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (!keywordsByChatId.isEmpty()) {
            for (String item : keywordsByChatId) {
                joinerKeywords.add(++counter + ". " + item);
            }
        }
        return joinerKeywords.toString();
    }

    // Показ ключевых слов в Телеграме (/keywords)
    public String showKeywordsList(long chatId) {
        int isPremium = userRepository.isPremiumByChatId(chatId);
        String joinerKeywords = getKeywordsList(chatId);

        String keywordsPeriod;
        if (isPremium != 1) {
            keywordsPeriod = "<b>" + settingsRepository.getKeywordsPeriod(chatId) + "</b>";
        } else if (settingsRepository.getPremiumSearchByChatId(chatId).equals("off")) {
            keywordsPeriod = "<b>" + settingsRepository.getKeywordsPeriod(chatId) + "</b> " + Common.ICON_PREMIUM_IS_ACTIVE;
        } else {
            keywordsPeriod = "<b>2 min</b> " + Common.ICON_PREMIUM_IS_ACTIVE;
        }

        if (joinerKeywords != null && joinerKeywords.length() != 0) {
            String auto = settingsRepository.getSchedulerOnOffByChatId(chatId);
            int theme = settingsRepository.getMessageTheme(chatId);
            String lang = settingsRepository.getLangByChatId(chatId);
            String onOffSchedulerRus = "/on";
            if (auto.contains("on") || auto.contains("включён")) onOffSchedulerRus = "/off";

            return String.format(
                    listKeywordsText,
                    joinerKeywords,
                    "<b>" + Common.setOnOffRus(auto, lang) + " </b>" + onOffSchedulerRus,
                    keywordsPeriod + " /interval",
                    "<b>" + theme + "</b> /theme"
            );
        } else {
            return setupKeywordsText;
        }
    }

    // Удаление ключевых слов
    public List<String> deleteKeywords(String keywordNumbers, long chatId) {
        List<String> messages = new ArrayList<>();
        ArrayList<String> words = new ArrayList<>();

        try {
            if (keywordNumbers.equals("*")) {
                words.add("*");
            } else {
                String[] nums = keywordNumbers.split(",");
                String[] split = getKeywordsList(chatId).split("\n");

                // Сопоставление
                for (String num : nums) {
                    int numInt = Integer.parseInt(num.trim());

                    for (String row : split) {
                        int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                        row = row.substring(row.indexOf(".") + 1);

                        if (numInt == rowNum) {
                            words.add(row);
                        }
                    }
                }
            }

            // Удаление
            for (String word : words) {
                word = word.trim().toLowerCase();

                if (word.equals("*")) {
                    keywordRepository.deleteAllKeywordsByChatId(chatId);
                    messages.add(deleteAllWordsText);
                    break;
                }

                if (keywordRepository.isKeywordExists(chatId, word) > 0) {
                    keywordRepository.deleteKeywordByChatId(chatId, word);
                    messages.add("❌ " + word);
                } else {
                    messages.add(String.format(wordIsNotInTheListText, word));
                }
            }
            messages.add(DONE);

            showKeywordsList(chatId);
        } catch (NumberFormatException n) {
            messages.add(allowCommasAndNumbersText);
        } catch (NullPointerException npe) {
            messages.add("click /keywords" + Common.TOP_TEN_SHOW_LIMIT);
        }
        return messages;
    }

}
