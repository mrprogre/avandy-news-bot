package com.avandy.bot.service;

import com.avandy.bot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class TelegramBotTests {
    private Update update;
    private ArgumentCaptor<SendMessage> argumentCaptor;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private ExcludingTermsRepository excludingTermsRepository;

    @Spy
    @InjectMocks
    private TelegramBot bot;

    @BeforeEach
    void before() {
        update = new Update();
        update.setMessage(new Message());
        update.getMessage().setChat(new Chat());
        update.getMessage().getChat().setId(111111L);
        update.getMessage().getChat().setFirstName("Dmitry");

        argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
    }

    @Test
    public void startTest() throws TelegramApiException {
        update.getMessage().setText("/start");
        bot.onUpdateReceived(update);
        verify(bot, new Times(1)).execute(argumentCaptor.capture());

        List<SendMessage> actual = argumentCaptor.getAllValues();
        assertThat(actual.get(0).getChatId())
                .isEqualTo(update.getMessage().getChatId().toString());
        assertThat(actual.get(0).getText())
                .contains("Choose your language");

        verify(userRepository, times(1))
                .isUserExistsByChatId(update.getMessage().getChatId());
        verify(settingsRepository, times(1))
                .save(any());
    }

    @Test
    public void keywordsTest() throws TelegramApiException {
        update.getMessage().setText("/keywords");
        bot.onUpdateReceived(update);
        verify(bot, new Times(1)).execute(argumentCaptor.capture());

        List<SendMessage> actual = argumentCaptor.getAllValues();
        assertThat(actual.get(0).getText())
                .isNotEmpty();

        verify(keywordRepository, times(1))
                .findKeywordsByChatId(anyLong());
    }

//    @Test
//    public void addKeywordsTest() throws TelegramApiException {
//        update.getMessage().setText("/add-keywords Россия, Москва, Ленина");
//        bot.onUpdateReceived(update);
//        verify(bot, new Times(2)).execute(argumentCaptor.capture());
//
//        List<SendMessage> actual = argumentCaptor.getAllValues();
//
//        verify(keywordRepository, times(3))
//                .save(any());
//        assertThat(actual.get(0).getText())
//                .contains("3 ✔");
//    }

    @Test
    public void excludedTest() throws TelegramApiException {
        update.getMessage().setText("/excluding");
        bot.onUpdateReceived(update);
        verify(bot, new Times(1)).execute(argumentCaptor.capture());

        List<SendMessage> actual = argumentCaptor.getAllValues();
        assertThat(actual.get(0).getText())
                .isNotEmpty();

        verify(excludingTermsRepository, times(1))
                .findExcludedByChatId(anyLong());
    }

//    @Test
//    public void addExcludedTest() throws TelegramApiException {
//        update.getMessage().setText("/add-excluded Россия, Москва, Ленина");
//        bot.onUpdateReceived(update);
//        verify(bot, new Times(2)).execute(argumentCaptor.capture());
//
//        List<SendMessage> actual = argumentCaptor.getAllValues();
//
//        verify(excludingTermsRepository, times(3))
//                .save(any());
//        assertThat(actual.get(0).getText())
//                .isNotEmpty();
//    }

}