package com.avandy.bot.service;

import com.avandy.bot.model.ExcludingTerm;
import com.avandy.bot.repository.ExcludingTermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.avandy.bot.bot.Text.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcludingTermsService {
    @Value("${bot.owner}")
    public long OWNER_ID;
    private final ExcludingTermsRepository excludingTermsRepository;
    private static final int OFFSET = 60;

    // Добавление слов-исключений
    public List<String> addExclude(long chatId, String[] list) {
        List<String> messages = new ArrayList<>();
        int counter = 0;
        List<String> excludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);

        ExcludingTerm excluded;
        for (String word : list) {
            word = word.trim().toLowerCase();

            if (word.length() >= 3 && word.length() <= 32) {
                if (!excludedByChatId.contains(word)) {
                    excluded = new ExcludingTerm();
                    excluded.setChatId(chatId);
                    excluded.setWord(word);

                    try {
                        excludingTermsRepository.save(excluded);
                        counter++;
                    } catch (Exception e) {
                        if (e.getMessage().contains("ui_excluded")) {
                            log.info(wordIsExistsText + word);
                        }
                    }

                } else {
                    messages.add(wordIsExistsText + word);
                }
            } else {
                messages.add(minWordLengthText2);
            }
        }

        if (counter != 0) {
            messages.add(addedExceptionWordsText + " - " + counter + " ✔️");
            messages.add(DONE);
        } else {
            messages.add(wordsIsNotAddedText);
        }
        return messages;
    }

    // Удаление слов-исключений
    public List<String> delExcluded(long chatId, String[] excluded) {
        List<String> messages = new ArrayList<>();

        for (String exclude : excluded) {
            exclude = exclude.trim().toLowerCase();

            if (exclude.equals("*")) {
                excludingTermsRepository.deleteAllExcludedByChatId(chatId);
                messages.add(deleteAllWordsText);
                return messages;
            }

            if (excludingTermsRepository.isWordExists(chatId, exclude) > 0) {
                excludingTermsRepository.deleteExcludedByChatId(chatId, exclude);
                messages.add("Удалено слово - " + exclude + " ❌");
            } else {
                messages.add("Слово " + exclude + " отсутствует в списке");
            }
        }
        return messages;
    }

    // Список слов-исключений (/excluding)
    public String getExcludedList(long chatId) {
        List<String> excludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);
        int excludedCount = excludedByChatId.size();

        if (!excludedByChatId.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            int counter = 0;
            for (String item : excludedByChatId) {
                joiner.add(item);
                if (++counter == OFFSET) break;
            }
            return "<b>" + exclusionWordsText + "</b> [" + excludedCount + "]\n" + joiner + ";" + excludedCount;
        } else {
            return "";
        }
    }

}
