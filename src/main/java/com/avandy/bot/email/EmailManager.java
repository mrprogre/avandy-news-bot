package com.avandy.bot.email;

import com.avandy.bot.service.Search;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class EmailManager {
    private static final String SEND_EMAIL_FROM = "avandy-news@mail.ru";
    private static final String EMAIL_PWD = System.getenv("MAIL_PWD");
    private final String today = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    private final String subject = ("News (" + today + ")");
    private final StringBuilder text = new StringBuilder();
    public static AtomicBoolean isSent = new AtomicBoolean(false);

    // Отправка письма
    public void sendMessage(List<String> headlinesList, String sendTo) {
        for (String s : headlinesList) {
            text.append(s).append("\n\n");
        }
        try {
            new Sender().send(subject, text.toString(),
                    SEND_EMAIL_FROM,
                    EMAIL_PWD,
                    sendTo);
            log.info("> e-mail sent successfully");
            isSent.set(true);
        }  catch (AuthenticationFailedException a) {
            log.warn("> incorrect email or password");
        } catch (MessagingException e) {
            log.warn("> email not sent: " + e.getMessage());
        }
    }

//    private void sendKeywordsToEmail(long chatId) {
//        String textNotExists = "Set up your email and keywords";
//        if (!settingsRepository.existsById(chatId)
//                && keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
//            sendMessage(chatId, textNotExists);
//            return;
//        } else if (!settingsRepository.existsById(chatId)) {
//            textNotExists = "Укажите свою электронную почту";
//            sendMessage(chatId, textNotExists);
//            return;
//        } else if (keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
//            textNotExists = setUpKeywords;
//            showAddKeywordsButton(chatId, textNotExists);
//            return;
//        }
//
//        // Search
//        search.start(chatId, "send", "keywords");
//
//        String text;
//        if (EmailManager.isSent.get()) {
//            text = EmojiParser.parseToUnicode(
//                    "Найдено: <b>" + Search.toEmailNewsCounter + "</b>. " +
//                            "E-mail sent \uD83C\uDF3F");
//            EmailManager.isSent.set(false);
//        } else {
//            text = EmojiParser.parseToUnicode(headlinesNotFound);
//        }
//        sendMessage(chatId, text);
//    }

}