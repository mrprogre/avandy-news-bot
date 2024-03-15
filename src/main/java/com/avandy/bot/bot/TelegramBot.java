package com.avandy.bot.bot;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.User;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.search.Search;
import com.avandy.bot.search.SearchService;
import com.avandy.bot.service.ExcludingTermsService;
import com.avandy.bot.service.KeywordsService;
import com.avandy.bot.utils.Common;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.avandy.bot.bot.Text.*;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${bot.token}")
    private String TOKEN;
    @Value("${bot.owner}")
    public long OWNER_ID;
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, StringBuilder> usersTop = new ConcurrentHashMap<>();
    private final Map<Long, Integer> usersTopPage = new ConcurrentHashMap<>();
    private final Map<Long, Integer> usersExclTermsPage = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> topData = new ConcurrentHashMap<>();
    // Full Search
    private final Map<Long, List<Headline>> newsListFullSearchData = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListFullSearchCounter = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListFullSearchMessageId = new ConcurrentHashMap<>();
    // Keywords Search
    private final Map<Long, List<Headline>> newsListKeySearchData = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListKeySearchCounter = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListKeySearchMessageId = new ConcurrentHashMap<>();
    // Top Search
    private final Map<Long, List<Headline>> newsListTopSearchData = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListTopSearchCounter = new ConcurrentHashMap<>();
    private final Map<Long, Integer> newsListTopSearchMessageId = new ConcurrentHashMap<>();
    //
    private final Map<Long, String> oneWordFromChat = new ConcurrentHashMap<>();
    // Repository
    private final SearchService searchService;
    private final BotConfig config;
    private final RssRepository rssRepository;
    private final UserRepository userRepository;
    private final TopTenRepository topRepository;
    private final KeywordRepository keywordRepository;
    private final SettingsRepository settingsRepository;
    private final ShowedNewsRepository showedNewsRepository;
    private final ExcludingTermsRepository excludingTermsRepository;
    private final KeywordsService keywordsService;
    private final ExcludingTermsService excludingTermsService;
    private final CategoryRepository categoryRepository;
    private final int offset = 60;
    private final int searchOffset = 5;
    private final int topSearchOffset = 3;
    private final String delimiterPages = " : ";

    @Autowired
    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config,
                       SearchService searchService, UserRepository userRepository, KeywordRepository keywordRepository,
                       SettingsRepository settingsRepository, ExcludingTermsRepository excludingTermsRepository,
                       RssRepository rssRepository, TopTenRepository topRepository,
                       ShowedNewsRepository showedNewsRepository, KeywordsService keywordsService,
                       ExcludingTermsService excludingTermsService, CategoryRepository categoryRepository) {
        super(botToken);
        this.config = config;
        this.searchService = searchService;
        this.userRepository = userRepository;
        this.keywordRepository = keywordRepository;
        this.settingsRepository = settingsRepository;
        this.excludingTermsRepository = excludingTermsRepository;
        this.rssRepository = rssRepository;
        this.topRepository = topRepository;
        this.showedNewsRepository = showedNewsRepository;
        this.keywordsService = keywordsService;
        this.excludingTermsService = excludingTermsService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Отправка отзыва с приложением скриншота
            if (update.hasMessage() && update.getMessage().hasPhoto()) {
                Long chatId = update.getMessage().getChatId();

                // Отправка отзыва/платежа с приложением скриншота
                if (UserState.SEND_FEEDBACK.equals(userStates.get(chatId)) ||
                        UserState.SEND_PAYMENT.equals(userStates.get(chatId))) {

                    sendFeedbackWithDocument(update);
                    userStates.remove(chatId);
                    sendMessage(chatId, sentText);
                }

                // Основные команды бота
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                String userTelegramLanguageCode = update.getMessage().getFrom().getLanguageCode();
                if (userTelegramLanguageCode == null) userTelegramLanguageCode = "ru";

                usersTopPage.putIfAbsent(chatId, 0);
                usersExclTermsPage.putIfAbsent(chatId, 0);
                newsListFullSearchCounter.putIfAbsent(chatId, 0);
                newsListTopSearchCounter.putIfAbsent(chatId, 0);
                newsListKeySearchCounter.putIfAbsent(chatId, 0);

                setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
                UserState userState = userStates.get(chatId);

                // DEBUG: запись действий пользователя для анализа
                if (OWNER_ID != chatId) {
                    if (userState != null)
                        log.warn("{}, {}: message: {}", chatId, userRepository.findNameByChatId(chatId),
                                userState + " " + messageText);
                    else log.warn("{}, {}: message: {}", chatId, userRepository.findNameByChatId(chatId), messageText);
                }

                // Принудительный выход
                List<String> flugengeheimen = List.of("стоп", "/стоп", "stop", "/stop");
                if (flugengeheimen.contains(messageText.toLowerCase())) {
                    userStates.remove(chatId);
                    nextKeyboard(chatId, actionCanceledText);
                }

                // SEND TO ALL RUS FROM BOT OWNER
                else if (messageText.startsWith(":") && config.getBotOwner() == chatId) {
                    String textToSend = messageText.substring(messageText.indexOf(" "));
                    List<User> users = userRepository.findAllRusByIsActive();
                    for (User user : users) {
                        sendMessageWithPreview(user.getChatId(), textToSend, null);
                    }
                }

                // SEND TO ALL ENG FROM BOT OWNER
                else if (messageText.startsWith("^") && config.getBotOwner() == chatId) {
                    String textToSend = messageText.substring(messageText.indexOf(" "));
                    List<User> users = userRepository.findAllNotRusByIsActive();
                    for (User user : users) {
                        sendMessageWithPreview(user.getChatId(), textToSend, null);
                    }

                    // SEND TO CHAT_ID FROM BOT OWNER
                } else if (messageText.startsWith("@") && config.getBotOwner() == chatId) {
                    long chatToSend = Long.parseLong(messageText.substring(1, messageText.indexOf(" ")));
                    String textToSend = messageText.substring(messageText.indexOf(" "));
                    sendMessageWithPreview(chatToSend, textToSend, null);

                    // DEACTIVATE SOURCE
                } else if (messageText.startsWith("-") && config.getBotOwner() == chatId) {
                    try {
                        String link = messageText.substring(messageText.indexOf("-") + 1, messageText.indexOf("="));
                        int value = Integer.parseInt(messageText.substring(messageText.indexOf("=") + 1));

                        int count = rssRepository.updateIsActiveRss(value, link);
                        if (count == 1) {
                            sendMessage(chatId, "Активность источника переключена на: " + value);
                        }
                    } catch (Exception e) {
                        sendMessage(chatId, "Exception: " + e.getMessage() + "\nФормат команды: -link=0 или 1");
                        return;
                    }

                    /* USER INPUT BLOCK */
                } else if (UserState.SEND_FEEDBACK.equals(userState)) {
                    String feedback = messageText.substring(messageText.indexOf(" ") + 1);
                    if (checkUserInput(chatId, messageText)) return;
                    sendFeedback(chatId, feedback);
                    sendMessage(chatId, sentText);

                    // Добавление ключевых слов
                } else if (UserState.ADD_KEYWORDS.equals(userState)) {
                    String keywords = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, keywords)) return;
                    boolean isAdded = addKeywords(chatId, keywords);
                    if (!isAdded) return;
                    showKeywordsList(chatId);

                    // Удаление ключевых слов (передаются порядковые номера, которые преобразуются в слова)
                } else if (UserState.DEL_KEYWORDS.equals(userState)) {
                    String keywords = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, keywords)) return;
                    boolean isDeleted = deleteKeywords(keywords, chatId);
                    if (!isDeleted) return;

                    // Добавление слов-исключений
                } else if (UserState.ADD_EXCLUDED.equals(userState)) {
                    String exclude = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, exclude)) return;
                    String[] words = exclude.split(",");
                    addExclude(chatId, words);

                    // Удаление слов-исключений
                } else if (UserState.DEL_EXCLUDED.equals(userState)) {
                    String excluded = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, excluded)) return;
                    String[] words = excluded.split(",");
                    delExcluded(chatId, words);
                    getExcludedList(chatId);

                    // Восстановление удалённых слов из показа в Топе
                } else if (UserState.DEL_TOP.equals(userState)) {
                    String text = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, text)) return;
                    String[] words = text.split(",");
                    topListDelete(chatId, words);
                    getTopTenWordsList(chatId);

                } else if (UserState.ADD_SOURCE.equals(userState)) {
                    String text = messageText.trim();
                    String status = addSource(chatId, userTelegramLanguageCode, text);
                    showStatus(chatId, List.of(status));

                } else if (UserState.DEL_SOURCE.equals(userState)) {
                    String source = messageText.trim().toLowerCase();
                    if (checkUserInput(chatId, source)) return;
                    deleteSourceRss(chatId, source);

                    // Поиск по трём кнопкам меню
                } else if (messageText.equals("По словам") || messageText.equals("Keywords")) {
                    new Thread(() -> findNewsByKeywordsManual(chatId)).start();

                } else if (messageText.equals("Top 20") || messageText.equals("Show top")) {
                    new Thread(() -> showTop(chatId)).start();

                } else if (messageText.equals("Все новости") || messageText.equals("All news")) {
                    new Thread(() -> fullSearch(chatId)).start();
                } else {
                    /* Основные команды */
                    switch (messageText) {
                        case "/settings" -> new Thread(() -> getSettings(chatId)).start();
                        case "/keywords", "/list_key" -> new Thread(() -> showKeywordsList(chatId)).start();
                        case "/search" -> new Thread(() -> initSearchesKeyboard(chatId)).start();
                        case "/info" -> new Thread(() -> infoKeyboard(chatId)).start();
                        case "/sources" -> new Thread(() -> sourcesKeyboard(chatId)).start();

                        case "/clear" -> {
                            showedNewsRepository.deleteHistoryManual(chatId);
                            sendMessage(chatId, historyClearText);
                        }
                        case "/theme" -> new Thread(() -> setThemeKeyboard(chatId)).start();

                        // Автопоиск
                        case "/on" -> {
                            settingsRepository.updateScheduler("on", chatId);
                            new Thread(() -> showKeywordsList(chatId)).start();
                        }
                        case "/off" -> {
                            settingsRepository.updateScheduler("off", chatId);
                            new Thread(() -> showKeywordsList(chatId)).start();
                        }
                        case "/interval" -> keywordsChangePeriodKeyboard(chatId);
                        case "/change_time" -> startSearchTimeButtons(chatId);

                        // Top 20
                        case "/interval_top" -> topPeriodsKeyboard(chatId);
                        case "/list_top" -> getTopTenWordsList(chatId);

                        // Полный поиск
                        case "/interval_full" -> fullSearchPeriodChangeKeyboard(chatId);
                        case "/filter_on" -> {
                            settingsRepository.updateExcluded("on", chatId);
                            savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                        }
                        case "/filter_off" -> {
                            settingsRepository.updateExcluded("off", chatId);
                            savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                        }

                        // Премиум
                        case "/prem_on" -> {
                            settingsRepository.updatePremiumSearch("on", chatId);
                            savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                        }
                        case "/prem_off" -> {
                            settingsRepository.updatePremiumSearch("off", chatId);
                            savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                        }

                        // Категории
                        case "/sport" -> addExclude(chatId, categoryRepository.getWords("sport"));
                        case "/celebrity" -> addExclude(chatId, categoryRepository.getWords("celebrity"));
                        case "/negative" -> addExclude(chatId, categoryRepository.getWords("negative"));
                        case "/policy" -> addExclude(chatId, categoryRepository.getWords("policy"));

                        // Misc
                        case "/start" -> startActions(update, chatId, userTelegramLanguageCode);
                        case "/excluding", "/list_ex" -> getExcludedList(chatId);
                        case "/delete" -> showYesNoOnDeleteUser(chatId);
                        case "/top" -> new Thread(() -> showTop(chatId)).start();
                        case "/premium" -> new Thread(() -> showYesNoGetPremium(chatId)).start();
                        default -> undefinedKeyboard(chatId, messageText);
                    }
                }
                userStates.remove(chatId);

                /* CALLBACK DATA */
            } else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
                usersTopPage.putIfAbsent(chatId, 0);
                usersExclTermsPage.putIfAbsent(chatId, 0);
                newsListFullSearchCounter.putIfAbsent(chatId, 0);
                newsListKeySearchCounter.putIfAbsent(chatId, 0);
                newsListTopSearchCounter.putIfAbsent(chatId, 0);

                /* DEBUG */
                if (OWNER_ID != chatId) {
                    log.warn("Callback: {}, chat_id: {}, {}", callbackData, chatId,
                            userRepository.findNameByChatId(chatId));
                }

                switch (callbackData) {
                    case "ADD_RSS" -> {
                        int isPremium = userRepository.isPremiumByChatId(chatId);
                        if (isPremium == 1) {
                            userStates.put(chatId, UserState.ADD_SOURCE);
                            cancelKeyboard(chatId, addSourceInfoText);
                        } else {
                            sendMessage(chatId, premiumAddSourcesText);
                        }
                    }
                    case "DEL_RSS" -> {
                        int isPremium = userRepository.isPremiumByChatId(chatId);
                        if (isPremium == 1) {
                            userStates.put(chatId, UserState.DEL_SOURCE);
                            cancelKeyboard(chatId, delSourceInfoText);
                        } else {
                            sendMessage(chatId, premiumAddSourcesText);
                        }
                    }

                    case "GET_PREMIUM" -> showYesNoGetPremium(chatId);
                    case "YES_PREMIUM" -> {
                        sendMessage(OWNER_ID, String.format(">>> Хочет премиум: %d, %s. Проверить оплату!", chatId,
                                userRepository.findNameByChatId(chatId)));
                        //sendMessage(chatId, getPremiumRequestText);
                        sendPaymentPremium(chatId, getPremiumRequestText);
                    }
                    case "PAYMENT" -> {
                        userStates.put(chatId, UserState.SEND_PAYMENT);
                        cancelKeyboard(chatId, sendMessageForDevText2);
                    }

                    case "NEXT_NEWS" -> getNewsListPage(chatId, searchOffset, messageId);
                    case "BEFORE_NEWS" -> getNewsListPage(chatId, -searchOffset, messageId);

                    case "NEXT_KEY" -> getNewsListKeyPage(chatId, searchOffset, messageId);
                    case "BEFORE_KEY" -> getNewsListKeyPage(chatId, -searchOffset, messageId);

                    case "NEXT_TOP" -> getNewsListTopPage(chatId, topSearchOffset, messageId);
                    case "BEFORE_TOP" -> getNewsListTopPage(chatId, -topSearchOffset, messageId);

                    case "FIRST_EXCL_PAGE" -> {
                        usersExclTermsPage.put(chatId, 0);
                        List<String> items = excludingTermsRepository.findExcludedByChatId(chatId);

                        List<String> terms = excludingTermsRepository.findExcludingTermsPage(chatId,
                                usersExclTermsPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, terms, listExcludedText);
                    }

                    case "LAST_EXCL_PAGE" -> {
                        List<String> items = excludingTermsRepository.findExcludedByChatId(chatId);
                        usersExclTermsPage.put(chatId, items.size() - offset);

                        List<String> terms = excludingTermsRepository.findExcludingTermsPage(chatId,
                                usersExclTermsPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, terms, listExcludedText);
                    }

                    case "NEXT_EXCL_PAGE" -> {
                        List<String> items = excludingTermsRepository.findExcludedByChatId(chatId);

                        Integer i = usersExclTermsPage.get(chatId);
                        usersExclTermsPage.put(chatId, i += offset);
                        if (i >= items.size()) usersExclTermsPage.put(chatId, i - offset);

                        List<String> terms = excludingTermsRepository.findExcludingTermsPage(chatId,
                                usersExclTermsPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, terms, listExcludedText);
                    }

                    case "BEFORE_EXCL_PAGE" -> {
                        List<String> items = excludingTermsRepository.findExcludedByChatId(chatId);

                        Integer i = usersExclTermsPage.get(chatId);
                        usersExclTermsPage.put(chatId, i -= offset);
                        if (i <= 0) usersExclTermsPage.put(chatId, 0);

                        List<String> terms = excludingTermsRepository.findExcludingTermsPage(chatId,
                                usersExclTermsPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, terms, listExcludedText);
                    }

                    case "FIRST_TOP_PAGE" -> {
                        usersTopPage.put(chatId, 0);
                        Set<String> items = topRepository.findAllByChatId(chatId);
                        List<String> excludedWordsPage = topRepository.getPage(chatId, usersTopPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, excludedWordsPage, listOfDeletedFromTopText);
                    }

                    case "LAST_TOP_PAGE" -> {
                        Set<String> items = topRepository.findAllByChatId(chatId);
                        usersTopPage.put(chatId, items.size() - offset);

                        List<String> excludedWordsPage = topRepository.getPage(chatId, usersTopPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, excludedWordsPage, listOfDeletedFromTopText);
                    }

                    case "NEXT_TOP_PAGE" -> {
                        Set<String> items = topRepository.findAllByChatId(chatId);
                        Integer i = usersTopPage.get(chatId);
                        usersTopPage.put(chatId, i += offset);
                        if (i >= items.size()) usersTopPage.put(chatId, i - offset);

                        List<String> excludedWordsPage = topRepository.getPage(chatId,
                                usersTopPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, excludedWordsPage, listOfDeletedFromTopText);
                    }

                    case "BEFORE_TOP_PAGE" -> {
                        Integer i = usersTopPage.get(chatId);
                        usersTopPage.put(chatId, i -= offset);
                        if (i <= 0) usersTopPage.put(chatId, 0);

                        Set<String> items = topRepository.findAllByChatId(chatId);
                        List<String> excludedWordsPage = topRepository.getPage(chatId,
                                usersTopPage.get(chatId), offset);
                        getExcludedWordsPage(chatId, messageId, items, excludedWordsPage, listOfDeletedFromTopText);
                    }

                    case "FEEDBACK" -> {
                        userStates.put(chatId, UserState.SEND_FEEDBACK);
                        cancelKeyboard(chatId, sendMessageForDevText);
                    }

                    /* KEYWORDS */
                    case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywordsManual(chatId)).start();
                    case "LIST_KEYWORDS" -> showKeywordsList(chatId);
                    case "ADD_KEYWORD" -> {
                        userStates.put(chatId, UserState.ADD_KEYWORDS);
                        cancelKeyboard(chatId, addInListText);
                    }
                    case "DELETE_KEYWORD" -> {
                        userStates.put(chatId, UserState.DEL_KEYWORDS);
                        cancelKeyboard(chatId, delFromListText + "\n* - " + removeAllText);
                    }

                    case "START_SEARCH", "DELETE_NO", "NO_PREMIUM" -> initSearchesKeyboard(chatId);

                    /* FULL SEARCH */
                    case "FIND_ALL" -> new Thread(() -> fullSearch(chatId)).start();

                    /* Поиск по одному слову, введённому пользователем, просто в общем чате */
                    case "SEARCH_BY_WORD" -> new Thread(() ->
                            wordSearch(chatId, oneWordFromChat.get(chatId), "chat")).start();

                    // добавить слово из чата в список ключевых слов
                    case "ADD_KEYWORD_FROM_CHAT" -> {
                        String words = oneWordFromChat.get(chatId).trim().toLowerCase();
                        addKeywords(chatId, words);
                        showKeywordsList(chatId);
                    }

                    /* EXCLUDED */
                    case "LIST_EXCLUDED" -> getExcludedList(chatId);
                    case "ADD_EXCLUDED" -> {
                        userStates.put(chatId, UserState.ADD_EXCLUDED);
                        cancelKeyboard(chatId, addInListText);
                    }
                    case "DELETE_EXCLUDED" -> {
                        userStates.put(chatId, UserState.DEL_EXCLUDED);
                        cancelKeyboard(chatId, delFromListText + "\n* - " + removeAllText);
                    }

                    /* TOP */
                    case "DEL_FROM_TOP" -> topDeleteKeyboard(chatId);
                    case "LIST_TOP" -> getTopTenWordsList(chatId);
                    case "GET_TOP" -> showTop(chatId);
                    case "SEARCH_BY_TOP_WORD" -> new Thread(() -> topSearchKeyboard(chatId)).start();
                    case "DELETE_TOP" -> {
                        userStates.put(chatId, UserState.DEL_TOP);
                        cancelKeyboard(chatId, removeFromTopTenListText);
                    }

                    /* SETTINGS */
                    case "GET_SETTINGS" -> getSettings(chatId);
                    case "SET_PERIOD" -> keywordsChangePeriodKeyboard(chatId);
                    case "SET_PERIOD_ALL" -> fullSearchPeriodChangeKeyboard(chatId);
                    case "SET_PERIOD_TOP" -> topPeriodsKeyboard(chatId);
                    case "SET_EXCLUDED" -> excludeOnOffKeyboard(chatId);
                    case "EXCLUDED_ON" -> {
                        settingsRepository.updateExcluded("on", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                    }
                    case "EXCLUDED_OFF" -> {
                        settingsRepository.updateExcluded("off", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                    }

                    case "JARO_WINKLER_MODE" -> showOnOffJaroWinklerKeyboard(chatId);
                    case "JARO_WINKLER_ON" -> {
                        settingsRepository.updateJaroWinkler("on", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                    }
                    case "JARO_WINKLER_OFF" -> {
                        settingsRepository.updateJaroWinkler("off", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                    }

                    case "CANCEL" -> {
                        userStates.remove(chatId);
                        nextKeyboard(chatId, actionCanceledText);
                    }

                    // Language
                    case "DE_BUTTON" -> setLangAndMenuCreate(chatId, "de");
                    case "ES_BUTTON" -> setLangAndMenuCreate(chatId, "es");
                    case "FR_BUTTON" -> setLangAndMenuCreate(chatId, "fr");
                    case "RU_BUTTON" -> setLangAndMenuCreate(chatId, "ru");
                    case "EN_BUTTON" -> setLangAndMenuCreate(chatId, "en");

                    // Обновление периода поиска по ключевым словам
                    case "BUTTON_1" -> keywordsUpdatePeriod(1, chatId);
                    case "BUTTON_2" -> keywordsUpdatePeriod(2, chatId);
                    case "BUTTON_4" -> keywordsUpdatePeriod(4, chatId);
                    case "BUTTON_6" -> keywordsUpdatePeriod(6, chatId);
                    case "BUTTON_8" -> keywordsUpdatePeriod(8, chatId);
                    case "BUTTON_12" -> keywordsUpdatePeriod(12, chatId);
                    case "BUTTON_24" -> keywordsUpdatePeriod(24, chatId);

                    // Обновление периода поиска по всем новостям
                    case "BUTTON_1_ALL" -> fullSearchPeriodUpdate(1, chatId);
                    case "BUTTON_2_ALL" -> fullSearchPeriodUpdate(2, chatId);
                    case "BUTTON_4_ALL" -> fullSearchPeriodUpdate(4, chatId);
                    case "BUTTON_6_ALL" -> fullSearchPeriodUpdate(6, chatId);
                    case "BUTTON_8_ALL" -> fullSearchPeriodUpdate(8, chatId);
                    case "BUTTON_12_ALL" -> fullSearchPeriodUpdate(12, chatId);
                    case "BUTTON_24_ALL" -> fullSearchPeriodUpdate(24, chatId);

                    // Обновление периода Топ 20
                    case "TOP_INTERVAL_1" -> topUpdatePeriod(1, chatId);
                    case "TOP_INTERVAL_2" -> topUpdatePeriod(2, chatId);
                    case "TOP_INTERVAL_4" -> topUpdatePeriod(4, chatId);
                    case "TOP_INTERVAL_6" -> topUpdatePeriod(6, chatId);
                    case "TOP_INTERVAL_8" -> topUpdatePeriod(8, chatId);
                    case "TOP_INTERVAL_12" -> topUpdatePeriod(12, chatId);
                    case "TOP_INTERVAL_24" -> topUpdatePeriod(24, chatId);

                    case "YES_BUTTON" -> addKeywordsKeyboardOrTop(chatId, yesButtonText);
                    case "NO_BUTTON" -> sendMessage(chatId, buyButtonText);
                    case "DELETE_YES" -> deleteUser(chatId);

                    case "TOP_NUM_1" -> searchNewsTop(1, chatId);
                    case "TOP_NUM_2" -> searchNewsTop(2, chatId);
                    case "TOP_NUM_3" -> searchNewsTop(3, chatId);
                    case "TOP_NUM_4" -> searchNewsTop(4, chatId);
                    case "TOP_NUM_5" -> searchNewsTop(5, chatId);
                    case "TOP_NUM_6" -> searchNewsTop(6, chatId);
                    case "TOP_NUM_7" -> searchNewsTop(7, chatId);
                    case "TOP_NUM_8" -> searchNewsTop(8, chatId);
                    case "TOP_NUM_9" -> searchNewsTop(9, chatId);
                    case "TOP_NUM_10" -> searchNewsTop(10, chatId);
                    case "TOP_NUM_11" -> searchNewsTop(11, chatId);
                    case "TOP_NUM_12" -> searchNewsTop(12, chatId);
                    case "TOP_NUM_13" -> searchNewsTop(13, chatId);
                    case "TOP_NUM_14" -> searchNewsTop(14, chatId);
                    case "TOP_NUM_15" -> searchNewsTop(15, chatId);
                    case "TOP_NUM_16" -> searchNewsTop(16, chatId);
                    case "TOP_NUM_17" -> searchNewsTop(17, chatId);
                    case "TOP_NUM_18" -> searchNewsTop(18, chatId);
                    case "TOP_NUM_19" -> searchNewsTop(19, chatId);
                    case "TOP_NUM_20" -> searchNewsTop(20, chatId);

                    case "TOP_DEL_1" -> topDelete(1, chatId);
                    case "TOP_DEL_2" -> topDelete(2, chatId);
                    case "TOP_DEL_3" -> topDelete(3, chatId);
                    case "TOP_DEL_4" -> topDelete(4, chatId);
                    case "TOP_DEL_5" -> topDelete(5, chatId);
                    case "TOP_DEL_6" -> topDelete(6, chatId);
                    case "TOP_DEL_7" -> topDelete(7, chatId);
                    case "TOP_DEL_8" -> topDelete(8, chatId);
                    case "TOP_DEL_9" -> topDelete(9, chatId);
                    case "TOP_DEL_10" -> topDelete(10, chatId);
                    case "TOP_DEL_11" -> topDelete(11, chatId);
                    case "TOP_DEL_12" -> topDelete(12, chatId);
                    case "TOP_DEL_13" -> topDelete(13, chatId);
                    case "TOP_DEL_14" -> topDelete(14, chatId);
                    case "TOP_DEL_15" -> topDelete(15, chatId);
                    case "TOP_DEL_16" -> topDelete(16, chatId);
                    case "TOP_DEL_17" -> topDelete(17, chatId);
                    case "TOP_DEL_18" -> topDelete(18, chatId);
                    case "TOP_DEL_19" -> topDelete(19, chatId);
                    case "TOP_DEL_20" -> topDelete(20, chatId);

                    /* AUTO SEARCH BY KEYWORDS */
                    case "SET_SCHEDULER" -> keywordsOnOffSchedulerKeyboard(chatId);
                    case "SCHEDULER_ON" -> {
                        settingsRepository.updateScheduler("on", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                    }
                    case "SCHEDULER_OFF" -> {
                        settingsRepository.updateScheduler("off", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                    }
                    case "SCHEDULER_START" -> startSearchTimeButtons(chatId);
                    // Set start time
                    case "SET_START_0" -> keywordsUpdateSearchStartTime(0, chatId);
                    case "SET_START_1" -> keywordsUpdateSearchStartTime(1, chatId);
                    case "SET_START_2" -> keywordsUpdateSearchStartTime(2, chatId);
                    case "SET_START_3" -> keywordsUpdateSearchStartTime(3, chatId);
                    case "SET_START_4" -> keywordsUpdateSearchStartTime(4, chatId);
                    case "SET_START_5" -> keywordsUpdateSearchStartTime(5, chatId);
                    case "SET_START_6" -> keywordsUpdateSearchStartTime(6, chatId);
                    case "SET_START_7" -> keywordsUpdateSearchStartTime(7, chatId);
                    case "SET_START_8" -> keywordsUpdateSearchStartTime(8, chatId);
                    case "SET_START_9" -> keywordsUpdateSearchStartTime(9, chatId);
                    case "SET_START_10" -> keywordsUpdateSearchStartTime(10, chatId);
                    case "SET_START_11" -> keywordsUpdateSearchStartTime(11, chatId);
                    case "SET_START_12" -> keywordsUpdateSearchStartTime(12, chatId);
                    case "SET_START_13" -> keywordsUpdateSearchStartTime(13, chatId);
                    case "SET_START_14" -> keywordsUpdateSearchStartTime(14, chatId);
                    case "SET_START_15" -> keywordsUpdateSearchStartTime(15, chatId);
                    case "SET_START_16" -> keywordsUpdateSearchStartTime(16, chatId);
                    case "SET_START_17" -> keywordsUpdateSearchStartTime(17, chatId);
                    case "SET_START_18" -> keywordsUpdateSearchStartTime(18, chatId);
                    case "SET_START_19" -> keywordsUpdateSearchStartTime(19, chatId);
                    case "SET_START_20" -> keywordsUpdateSearchStartTime(20, chatId);
                    case "SET_START_21" -> keywordsUpdateSearchStartTime(21, chatId);
                    case "SET_START_22" -> keywordsUpdateSearchStartTime(22, chatId);
                    case "SET_START_23" -> keywordsUpdateSearchStartTime(23, chatId);

                    // Премиум поиск каждые 2 минуты
                    case "PREMIUM_SEARCH" -> premiumSearchOnOffSchedulerKeyboard(chatId);
                    case "PREMIUM_SEARCH_ON" -> {
                        settingsRepository.updatePremiumSearch("on", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "on"));
                    }
                    case "PREMIUM_SEARCH_OFF" -> {
                        settingsRepository.updatePremiumSearch("off", chatId);
                        savedKeyboard(chatId, String.format(changesSavedText2, "off"));
                    }

                    case "MESSAGE_THEME_1" -> updateMessageTheme(chatId, 1);
                    case "MESSAGE_THEME_2" -> updateMessageTheme(chatId, 2);
                    case "MESSAGE_THEME_3" -> updateMessageTheme(chatId, 3);
                    case "MESSAGE_THEME_4" -> updateMessageTheme(chatId, 4);
                    case "MESSAGE_THEME_5" -> updateMessageTheme(chatId, 5);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Update exception: " + e.getMessage());
        }
    }


    // Добавление ключевых слов для поиска
    private boolean addKeywords(Long chatId, String keywords) {
        showStatus(chatId, keywordsService.addKeywords(chatId, new HashSet<>(Arrays.asList(keywords.split(",")))));
        return true;
    }

    public String addSource(long chatId, String userTelegramLanguageCode, String text) {
        if (!text.contains(",")) {
            return noCommasText;
        }
        if (checkUserInput(chatId, text)) return ERROR;
        String[] words = text.split(",");
        String country = words[0].trim();
        String source = words[1].trim();
        String link = words[2].trim();
        String parseType = "rss";

        boolean isParsedJsoup = Common.checkRssJsoup(link);
        boolean isParsedRome = Common.checkRssRome(link);

        if (!isParsedRome && isParsedJsoup) parseType = "no-rss";
        log.warn("isParsedRome = " + isParsedRome + ", isParsedJsoup = " + isParsedJsoup); // DEBUG

        if (isParsedRome || isParsedJsoup) {
            Integer isExists = rssRepository.isExistsByChatId(link);
            if (isExists != null) {
                return rssExistsText;
            }
            rssRepository.save(new RssList(chatId, country, source, link, 1, 100,
                    new Timestamp(System.currentTimeMillis()), parseType, userTelegramLanguageCode));
            savedKeyboard(chatId, changesSavedText);
        } else {
            return notSupportedRssText;
        }
        return DONE;
    }

    private void deleteSourceRss(long chatId, String source) {
        List<String> personalSources = rssRepository.findPersonalSources(chatId);
        if (personalSources.stream().anyMatch(x -> x.toLowerCase().equals(source))) {
            Integer isExists = rssRepository.isExistsPersonalRssByName(chatId, source);
            if (isExists != null) {
                int count = rssRepository.deletePersonalRss(chatId, source);
                if (count > 0) {
                    sendMessage(chatId, deleteText);
                }
            } else {
                sendMessage(chatId, rssNameNotExistsText);
            }
        } else {
            Integer isExists = rssRepository.isExistsCommonRssByName(source);
            if (isExists != null) {
                int count = rssRepository.deleteCommonRss(chatId, source);
                if (count > 0) {
                    sendMessage(chatId, deleteText);
                }
            } else {
                sendMessage(chatId, rssNameNotExistsText);
            }
        }
    }

    private void updateMessageTheme(Long chatId, int value) {
        settingsRepository.updateMessageTheme(value, chatId);
        savedKeyboard(chatId, String.format(changesSavedText3, value));
    }

    // Пользователь нажимает кнопку добавления текста, но передумывает и нажимает на команду,
    // которая как слово добавляется в БД
    private boolean checkUserInput(long chatId, String text) {

        if (text.startsWith("/")) {
            userStates.remove(chatId);
            sendMessage(chatId, inputExceptionText + text);
            return true;
        }

        if (text.equalsIgnoreCase(keywordsSearchText) ||
                text.equalsIgnoreCase(updateTopText2) ||
                text.equalsIgnoreCase(fullSearchText)) {
            userStates.remove(chatId);
            sendMessage(chatId, inputExceptionText2);
            return true;
        }

        return false;
    }

    /* KEYWORDS */
    // Ручной поиск по ключевым словам
    public void findNewsByKeywordsManual(long chatId) {
        String nameByChatId = userRepository.findNameByChatId(chatId);

        // DEBUG
        if (OWNER_ID != chatId) {
            log.warn("Ручной запуск поиска по словам: {}, {}", chatId, nameByChatId);
        }

        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (keywordsByChatId.isEmpty()) {
            addKeywordsKeyboard(chatId, setupKeywordsText);
            return;
        }

        getReplyKeyboard(chatId, String.format(searchByKeywordsStartText, settingsRepository.getKeywordsPeriod(chatId)));

        // Search
        List<Headline> headlines = searchService.start(chatId, "keywords");

        int counterParts = 1;
        if (headlines.size() > 0) {
            // INFO
            if (chatId != OWNER_ID) {
                log.warn("Найдено: {}, {}, новостей {}", chatId, nameByChatId, headlines.size());
            }

            newsListKeySearchCounter.put(chatId, 0);
            newsListKeySearchData.put(chatId, headlines);

            StringJoiner joiner = new StringJoiner(getDelimiterNews(chatId));
            for (Headline headline : newsListKeySearchData.get(chatId)) {
                joiner.add(getHeadlinesText(chatId, headline));

                if (counterParts++ == searchOffset) break;
            }

            if (counterParts != 0) {
                getReplyKeyboard(chatId, foundNewsText + ": <b>" + headlines.size() + "</b> " +
                        Common.ICON_NEWS_FOUNDED);
            }

            afterKeywordsSearchKeyboard(chatId, String.valueOf(joiner), headlines.size());
        } else {
            afterKeywordsSearchNotFoundKeyboard(chatId, headlinesNotFound);
        }
    }

    // Меняющееся сообщение со всеми новостями по страницам
    private void getNewsListKeyPage(Long chatId, int plusMinus, int messageId) {
        newsListKeySearchMessageId.put(chatId, messageId);
        List<Headline> headlines = newsListKeySearchData.get(chatId);
        if (headlines == null) return;
        messageId = newsListKeySearchMessageId.get(chatId);

        Integer i = newsListKeySearchCounter.get(chatId);
        i += plusMinus;
        newsListKeySearchCounter.put(chatId, i);

        int current = newsListKeySearchCounter.get(chatId);
        if (current >= headlines.size()) {
            current = current - searchOffset;
        }

        int next = newsListKeySearchCounter.get(chatId) + searchOffset;

        if (current < 0) {
            current = 0;
            next = searchOffset;
            newsListKeySearchCounter.put(chatId, 0);
        }

        if (next >= headlines.size()) {
            next = headlines.size();
            newsListKeySearchCounter.put(chatId, current);
        }

        int counterParts = 1;

        StringJoiner joiner = getHeadlinesText(headlines, current, next, counterParts, searchOffset, chatId);
        getEditMessage(chatId, messageId, joiner, afterKeywordsSearchKeyboard(chatId, "", headlines.size()));
    }

    // Автоматический поиск по ключевым словам
    public void autoSearchNewsByKeywords(long chatId) {
        List<String> keywordsByChatId = Collections.emptyList();
        try {
            String nameByChatId = userRepository.findNameByChatId(chatId);
            String keywordsPeriod = settingsRepository.getKeywordsPeriod(chatId);
            keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);
            //List<Long> premiumChatIds = userRepository.findAllPremiumUsersChatId();

            if (keywordsByChatId.isEmpty()) {
                return;
            }

            // Search
            List<Headline> headlines = searchService.start(chatId, "keywords");

            int counterParts = 1;
            if (headlines.size() > 0) {
                // INFO
                log.info("Автопоиск: {}, {}, найдено {} за {}", chatId, nameByChatId, headlines.size(), keywordsPeriod);

                /* DEBUG PREMIUM USERS */
//            for (Long id : premiumChatIds) {
//                if (id == chatId) {
//                    log.warn("# Автопоиск для премиум пользователя {}, {}. Найдено новостей: {}",
//                            id, userRepository.findNameByChatId(chatId), headlines.size());
//                }
//            }

                if (headlines.size() > Common.LIMIT_FOR_BREAKING_INTO_PARTS) {
                    // 10 message in 1
                    StringJoiner joiner = new StringJoiner(getDelimiterNews(chatId));

                    for (Headline headline : headlines) {
                        joiner.add(getHeadlinesText(chatId, headline));

                        if (counterParts == 10) {
                            sendMessage(chatId, String.valueOf(joiner));
                            joiner = new StringJoiner(getDelimiterNews(chatId));
                            counterParts = 0;
                        }
                        counterParts++;
                    }

                    // отправить оставшиеся новости
                    if (counterParts != 0) {
                        sendMessage(chatId, String.valueOf(joiner));
                    }

                } else {
                    for (Headline headline : headlines) {
                        String text = getHeadlinesText(chatId, headline);

                        sendMessage(chatId, text);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error autoSearchNewsByKeywords: {}\n {}, keywords {}", e.getMessage(), chatId, keywordsByChatId);
        }
    }

    // Показ ключевых слов в Телеграме (/keywords)
    private void showKeywordsList(long chatId) {
        String keywords = keywordsService.showKeywordsList(chatId);

        if (keywords != null && keywords.length() != 0) {
            keywordsListKeyboard(chatId, keywords);
        } else {
            addKeywordsKeyboard(chatId, setupKeywordsText);
        }
    }

    // Изменение периода поиска
    private void keywordsUpdatePeriod(int period, long chatId) {
        settingsRepository.updateKeywordsPeriod(period + "h", chatId);
        savedKeyboard(chatId, String.format(changesSavedText2, period + "h"));
    }

    // Удаление ключевых слов. Порядковые номера преобразуются в слова, которые потом и удаляются из БД
    private boolean deleteKeywords(String keywordNumbers, long chatId) {
        showStatus(chatId, keywordsService.deleteKeywords(keywordNumbers, chatId));
        return true;
    }

    // Изменение времени старта запуска автопоиска по ключевым словам
    private void keywordsUpdateSearchStartTime(int start, long chatId) {
        LocalTime time = LocalTime.of(start, 0, 0);
        settingsRepository.updateStart(time, chatId);
        savedKeyboard(chatId, String.format(changesSavedText2, time));
    }

    // KEYBOARDS
    private void addKeywordsKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD_KEYWORD", addText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    private void addKeywordsKeyboardOrTop(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText2);
        buttons.put("ADD_KEYWORD", addText2);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    public void keywordsChangePeriodKeyboard(long chatId) {
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.maker(Buttons.getChangePeriodKeywords()));
    }

    private void keywordsListKeyboard(long chatId, String text) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("DELETE_KEYWORD", delText);
        buttons1.put("ADD_KEYWORD", addText);
        buttons2.put("SET_PERIOD", intervalText);
        buttons2.put("FIND_BY_KEYWORDS", searchText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
    }

    // Кнопки включения и выключения авто запуска автопоиска
    private void keywordsOnOffSchedulerKeyboard(long chatId) {
        sendMessage(chatId, autoSearchText, InlineKeyboards.maker(Buttons.getAutoSearchOnOff()));
    }

    // Кнопки включения и выключения авто запуска автопоиска
    private void premiumSearchOnOffSchedulerKeyboard(long chatId) {
        sendMessage(chatId, premiumSearchSettingsText, InlineKeyboards.maker(Buttons.getOnOffPremiumSearch()));
    }

    // Кнопки для выбора времени старта автопоиска (в часах)
    public void startSearchTimeButtons(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();

        for (int x = 0; x <= 23; x++) {
            if (x <= 5)
                buttons1.put("SET_START_" + x, String.valueOf(x));
            else if (x <= 11)
                buttons2.put("SET_START_" + x, String.valueOf(x));
            else if (x <= 17)
                buttons3.put("SET_START_" + x, String.valueOf(x));
            else
                buttons4.put("SET_START_" + x, String.valueOf(x));
        }

        sendMessage(chatId, chooseSearchStartText, InlineKeyboards.maker(buttons1, buttons2, buttons3, buttons4, null));
    }

    // Кнопки после итогов поиска по ключевым словам
    private InlineKeyboardMarkup afterKeywordsSearchKeyboard(long chatId, String text, int wordsCount) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("SET_PERIOD", intervalText);
        buttons1.put("LIST_KEYWORDS", listText);
        buttons1.put("FIND_BY_KEYWORDS", searchText);

        int i = newsListKeySearchCounter.get(chatId) + searchOffset + 1;

        if (wordsCount > searchOffset) {
            buttons2.put("BEFORE_KEY", "« «");
            buttons2.put("TOTAL_KEY", (int) Math.ceil((double) (i - 1) / searchOffset) + delimiterPages +
                    (int) Math.ceil((double) wordsCount / searchOffset));
            buttons2.put("NEXT_KEY", "» »");
        }

        if (!text.isBlank()) {
            sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
            return null;
        }

        return InlineKeyboards.maker(buttons1, buttons2, null, null, null);
    }

    private void afterKeywordsSearchNotFoundKeyboard(long chatId, String text) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();

        buttons1.put("SET_PERIOD", intervalText2);
        buttons2.put("LIST_KEYWORDS", listText3);
        buttons3.put("FIND_BY_KEYWORDS", searchText2);
        buttons4.put("START_SEARCH", findSelectText);

        sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, buttons3, buttons4, null));
    }

    /* FULL SEARCH */
    // Полный поиск
    private void fullSearch(long chatId) {
        // DEBUG
        if (OWNER_ID != chatId) {
            log.warn("{}: Запуск полного поиска", chatId);
        }

        getReplyKeyboard(chatId, String.format(fullSearchStartText, settingsRepository.getFullSearchPeriod(chatId)));
        //sendMessage(chatId, fullSearchStartText);

        // Поиск и заполнение коллекции
        List<Headline> headlines = searchService.start(chatId, "all");

        int counterParts = 1;
        if (headlines.size() > 0) {
            newsListFullSearchCounter.put(chatId, 0);
            newsListFullSearchData.put(chatId, headlines);
            StringJoiner joiner = new StringJoiner(getDelimiterNews(chatId));
            for (Headline headline : newsListFullSearchData.get(chatId)) {
                joiner.add(getHeadlinesText(chatId, headline));

                if (counterParts++ == searchOffset) break;
            }

            if (counterParts != 0) {
                sendMessage(chatId, foundNewsText + " <b>" + headlines.size() + "</b> (" +
                        excludedNewsText + " <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>) " +
                        Common.ICON_NEWS_FOUNDED);
            }

            afterFullSearchKeyboard(chatId, String.valueOf(joiner), headlines.size());
        } else {
            afterFullSearchKeyboard(chatId, headlinesNotFound, 0);
        }
    }

    // Меняющееся сообщение со всеми новостями по страницам
    private void getNewsListPage(Long chatId, int plusMinus, int messageId) {
        newsListFullSearchMessageId.put(chatId, messageId);
        List<Headline> headlines = newsListFullSearchData.get(chatId);
        if (headlines == null) return;
        messageId = newsListFullSearchMessageId.get(chatId);

        Integer i = newsListFullSearchCounter.get(chatId);
        i += plusMinus;
        newsListFullSearchCounter.put(chatId, i);

        int current = newsListFullSearchCounter.get(chatId);
        if (current >= headlines.size()) {
            current = current - searchOffset;
        }

        int next = newsListFullSearchCounter.get(chatId) + searchOffset;

        if (current < 0) {
            current = 0;
            next = searchOffset;
            newsListFullSearchCounter.put(chatId, 0);
        }

        if (next >= headlines.size()) {
            next = headlines.size();
            newsListFullSearchCounter.put(chatId, current);
        }

        int counterParts = 1;

        StringJoiner joiner = getHeadlinesText(headlines, current, next, counterParts, searchOffset, chatId);
        getEditMessage(chatId, messageId, joiner, afterFullSearchKeyboard(chatId, "", headlines.size()));
    }

    // Изменение глубины полного поиска
    private void fullSearchPeriodUpdate(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        savedKeyboard(chatId, String.format(changesSavedText2, period + "h"));
    }

    // KEYBOARDS
    // Кнопки выбора глубины полного поиска
    public void fullSearchPeriodChangeKeyboard(long chatId) {
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.maker(Buttons.getFullSearchPeriod()));
    }

    /* EXCLUDING TERMS */
    // Список слов-исключений (/excluding)
    private void getExcludedList(long chatId) {
        String excludedList = excludingTermsService.getExcludedList(chatId);

        if (!excludedList.isBlank()) {
            String[] split = excludedList.split(";");
            excludeListKeyboard(chatId, split[0], Integer.parseInt(split[1]));
        } else {
            excludeKeyboard(chatId);
        }
    }

    // Добавление слов-исключений
    private void addExclude(long chatId, String[] list) {
        List<String> messages = excludingTermsService.addExclude(chatId, list);
        showStatus(chatId, messages);
        getExcludedList(chatId);
    }

    // Удаление слов-исключений
    private void delExcluded(long chatId, String[] excluded) {
        List<String> messages = excludingTermsService.delExcluded(chatId, excluded);
        showStatus(chatId, messages);
    }

    // KEYBOARDS
    // Кнопки полного поиска с кнопками пагинации
    private InlineKeyboardMarkup afterFullSearchKeyboard(long chatId, String text, int wordsCount) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        int i = newsListFullSearchCounter.get(chatId) + searchOffset + 1;

        buttons1.put("SET_PERIOD_ALL", intervalText);
        buttons1.put("ADD_EXCLUDED", excludeWordText);
        buttons1.put("FIND_ALL", searchText);

        if (wordsCount > searchOffset) {
            buttons2.put("BEFORE_NEWS", "« «");
            buttons2.put("TOTAL_NEWS", (int) Math.ceil((double) (i - 1) / searchOffset) + delimiterPages +
                    (int) Math.ceil((double) wordsCount / searchOffset));
            buttons2.put("NEXT_NEWS", "» »");
        }

        if (!text.isBlank()) {
            sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
            return null;
        }

        return InlineKeyboards.maker(buttons1, buttons2, null, null, null);
    }

    // Кнопки включения и выключения механизма исключения заголовков
    public void excludeOnOffKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDED_OFF", "Off");
        buttons.put("EXCLUDED_ON", "On");
        sendMessage(chatId, exclusionText, InlineKeyboards.maker(buttons));
    }

    // Кнопки для просмотра списка слов-исключений
    private InlineKeyboardMarkup excludeListKeyboard(long chatId, String text, int wordsCount) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("DELETE_EXCLUDED", delText);
        buttons1.put("ADD_EXCLUDED", addText);
        buttons1.put("FIND_ALL", searchText);

        int i = usersExclTermsPage.get(chatId) + offset + 1;

        if (wordsCount > offset) {
            buttons2.put("FIRST_EXCL_PAGE", "««");
            buttons2.put("BEFORE_EXCL_PAGE", "«");
            buttons2.put("TOP_PAGE", (int) Math.ceil((double) (i - 1) / offset) + delimiterPages +
                    (int) Math.ceil((double) wordsCount / offset));
            buttons2.put("NEXT_EXCL_PAGE", "»");
            buttons2.put("LAST_EXCL_PAGE", "»»");
        }

        if (!text.isBlank()) {
            sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
            return null;
        }
        return InlineKeyboards.maker(buttons1, buttons2, null, null, null);
    }

    // Одна кнопка с добавлением в слова-исключения
    private void excludeKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD_EXCLUDED", addText);
        sendMessage(chatId, excludedWordsNotSetText, InlineKeyboards.maker(buttons));
    }

    /* TOP */
    // Показать Топ 20 (/top)
    private void showTop(long chatId) {
        // init
        int x = 1;
        searchService.start(chatId, "top");

        List<String> topTen = topWordsPrepare(chatId);
        if (topTen.size() > 0) {
            StringBuilder stringBuilderTop = new StringBuilder();
            String point = ".    ";

            for (String s : topTen) {
                if (x >= 10) point = ".  ";
                stringBuilderTop.append(x++).append(point).append(s);
            }
            usersTop.put(chatId, stringBuilderTop);

            String period = settingsRepository.getTopPeriod(chatId);

            topActionsKeyboard(chatId, String.format("%s<b>%s</b> " + Common.ICON_TOP_20_FIRE + " \n%s", top20ByPeriodText,
                    period, stringBuilderTop));

        } else {
            topKeyboard(chatId, updateTopText);
        }
    }


    // Поиск новостей по слову из Топа
    private void searchNewsTop(int wordNum, long chatId) {
        try {
            String word = getWordByNumber(wordNum, chatId);
            wordSearch(chatId, word, "top");
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
    }

    // Механизм поиска по слову из Топа
    private void wordSearch(long chatId, String word, String type) {
        // DEBUG
        if (OWNER_ID != chatId) {
            if (type.equals("top")) {
                log.warn("{}: Top 20 поиск по слову: {}", chatId, word);
            } else {
                log.warn("{}: Поиск в чате по слову: {}", chatId, word);
            }
        }

        newsListTopSearchCounter.put(chatId, 0);

        List<Headline> headlines;
        if (type.equals("top")) {
            headlines = searchService.start(chatId, word);
        } else {
            headlines = searchService.start(chatId, "chat" + word);
        }
        newsListTopSearchData.put(chatId, headlines);

        int counterParts = 1;
        if (headlines.size() > 0) {
            StringJoiner joiner = new StringJoiner(getDelimiterNews(chatId));
            for (Headline headline : newsListTopSearchData.get(chatId)) {
                joiner.add(getHeadlinesText(chatId, headline));

                if (counterParts++ == topSearchOffset) break;
            }

            if (counterParts != 0) {
                if (type.equals("top")) {
                    Integer id = newsListTopSearchMessageId.get(chatId);
                    if (id == null) {
                        afterTopSearchKeyboard(chatId, String.valueOf(joiner), headlines.size());
                    } else {
                        getNewsListTopPage(chatId, 0, id);
                    }
                } else if (type.equals("chat")) {
                    afterTopSearchKeyboard(chatId, String.valueOf(joiner), headlines.size());
                }
            }

            //topSearchKeyboard(chatId);
        } else {
            nextKeyboard(chatId, headlinesNotFound);
        }
    }

    // Меняющееся сообщение со всеми новостями по страницам
    private void getNewsListTopPage(Long chatId, int plusMinus, int messageId) {
        List<Headline> headlines = newsListTopSearchData.get(chatId);
        if (headlines == null) return;

        Integer i = newsListTopSearchCounter.get(chatId);
        i += plusMinus;
        newsListTopSearchCounter.put(chatId, i);

        int current = newsListTopSearchCounter.get(chatId);
        if (current >= headlines.size()) {
            current = current - topSearchOffset;
        }

        int next = newsListTopSearchCounter.get(chatId) + topSearchOffset;

        if (current < 0) {
            current = 0;
            next = topSearchOffset;
            newsListTopSearchCounter.put(chatId, 0);
        }

        if (next >= headlines.size()) {
            next = headlines.size();
            newsListTopSearchCounter.put(chatId, current);
        }

        int counterParts = 1;

        StringJoiner joiner = getHeadlinesText(headlines, current, next, counterParts, topSearchOffset, chatId);
        getEditMessage(chatId, messageId, joiner, afterTopSearchKeyboard(chatId, "", headlines.size()));
    }

    // Формирование списка слов для Топа
    private List<String> topWordsPrepare(long chatId) {
        Map<String, Integer> wordsCount = new HashMap<>();

        // Разбивка заголовков на слова и счёт их повторений
        for (Headline headline : Search.headlinesTopTen) {
            // Удаление ненужных знаков
            String[] titles = headline.getTitle()
                    .replaceAll(Common.REPLACE_ALL_TOP, "")
                    .toLowerCase()
                    .split(" ");

            for (String word : titles) {
                if (word.length() > 2) {
                    wordsCount.put(word, wordsCount.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Отсев слов, число повторений которых меньше трёх
        wordsCount = wordsCount.entrySet().stream()
                .filter(x -> x.getValue() >= 3)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Проверка включен ли метод Джаро-Винклера (в интерфейсе для простоты назван "Удаление окончаний")
        if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId))) {
            Common.fillTopWithoutDuplicates(wordsCount, Common.JARO_WINKLER_LEVEL);
        }

        // Удаление исключённых слов из мап для анализа
        Set<String> excludedWordsFromAnalysis = topRepository.findAllByChatId(chatId);
        for (String word : excludedWordsFromAnalysis) {
            wordsCount.remove(word);
        }

        List<String> list = wordsCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(Common.TOP_TEN_SHOW_LIMIT)
                .map(x -> String.format("%s [<b>%d</b>]", x.getKey(), x.getValue()) + "\n")
                .toList();
        topData.put(chatId, list);

        return list;
    }

    // Преобразование порядковых номеров слов в списке в слова
    private String getWordByNumber(int wordNum, long chatId) {
        String word = "";

        String[] split = usersTop.get(chatId).toString().split("\n");

        for (String row : split) {
            int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
            row = row.replaceAll("\\s", "");
            row = row.substring(row.indexOf(".") + 1, row.indexOf("["));

            if (wordNum == rowNum) {
                word = row;
            }
        }
        return word;
    }

    // Список удалённых слов из Топа
    private void getTopTenWordsList(long chatId) {
        Set<String> items = topRepository.findAllByChatId(chatId);

        getPaginationKeyboard(chatId, "<b>" + listOfDeletedFromTopText + "</b> [" + items.size() + "]\n" +
                        items.stream()
                                .limit(offset)
                                .toList()
                                .toString()
                                .replace("[", "")
                                .replace("]", ""),
                items.size()
        );
    }

    // Быстрое удаление слова из показа в Топе по кнопке
    private void topDelete(int wordNum, long chatId) {
        try {
            String word = getWordByNumber(wordNum, chatId);
            Set<String> topTenWordsByChatId = topRepository.findAllByChatId(chatId);

            TopExcluded topExcluded;
            if (!topTenWordsByChatId.contains(word)) {
                topExcluded = new TopExcluded();
                topExcluded.setChatId(chatId);
                topExcluded.setWord(word);

                try {
                    topRepository.save(topExcluded);
                    sendMessage(chatId, "❌ " + word);
                } catch (Exception e) {
                    if (e.getMessage().contains("ui_top_ten_excluded")) {
                        log.info(wordIsExistsText + word);
                    }
                }

            } else {
                sendMessage(chatId, wordIsExistsText + word);
            }
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
    }

    // Обновление интервала поиска слов для Топа
    private void topUpdatePeriod(int period, long chatId) {
        settingsRepository.updatePeriodTop(period + "h", chatId);
        savedKeyboard(chatId, String.format(changesSavedText2, period + "h"));
        showTop(chatId);
    }

    // Удаление слов из списка удалённых из Топа
    private void topListDelete(long chatId, String[] words) {
        for (String word : words) {
            word = word.trim().toLowerCase();

            if (topRepository.isWordExists(chatId, word) > 0) {
                topRepository.deleteWordByChatId(chatId, word);
                sendMessage(chatId, "❌ " + word);
            } else {
                sendMessage(chatId, String.format(wordIsNotInTheListText, word));
            }
        }
    }

    // Меняющееся сообщение со списками слов из Топа по страницам
    private void getExcludedWordsPage(Long chatId, Integer msg, Collection<String> items, List<String> words, String title) {
        String text = "<b>" + title + "</b> [" + items.size() + "]\n" +
                words.stream()
                        .toList()
                        .toString()
                        .replace("[", "")
                        .replace("]", "");

        EditMessageText editOptions = new EditMessageText();
        editOptions.setChatId(chatId);
        editOptions.setMessageId(msg);
        editOptions.setText(text);
        editOptions.enableHtml(true);

        if (title.equals(listOfDeletedFromTopText)) {
            editOptions.setReplyMarkup(getPaginationKeyboard(chatId, "", items.size()));
        } else {
            editOptions.setReplyMarkup(excludeListKeyboard(chatId, "", items.size()));
        }

        try {
            execute(editOptions);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("message is not modified")) {
                log.debug("[400] Bad Request: message is not modified");
            } else {
                log.error(e.getMessage());
            }
        }
    }

    // KEYBOARDS
    // Основные кнопки Топа
    private void topActionsKeyboard(long chatId, String text) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("JARO_WINKLER_MODE", jaroWinklerText);
        buttons1.put("LIST_TOP", excludedListText);
        buttons1.put("GET_TOP", updateTopText);

        buttons2.put("SET_PERIOD_TOP", intervalText);
        buttons2.put("DEL_FROM_TOP", delFromTopText);
        buttons2.put("SEARCH_BY_TOP_WORD", searchText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
    }

    // Кнопки со словами для быстрого поиска
    public void topSearchKeyboard(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : topData.get(chatId)) {
            s = s.substring(0, s.indexOf(" "));

            if (x <= 4)
                buttons1.put("TOP_NUM_" + x++, s);
            else if (x <= 8)
                buttons2.put("TOP_NUM_" + x++, s);
            else if (x <= 12)
                buttons3.put("TOP_NUM_" + x++, s);
            else if (x <= 16)
                buttons4.put("TOP_NUM_" + x++, s);
            else if (x <= 20)
                buttons5.put("TOP_NUM_" + x++, s);
        }
        sendMessage(chatId, chooseNumberWordFromTop, InlineKeyboards.maker(buttons1, buttons2, buttons3, buttons4, buttons5));
        newsListTopSearchMessageId.remove(chatId);
    }

    // Кнопки поиска по слову в Топ с кнопками пагинации
    private InlineKeyboardMarkup afterTopSearchKeyboard(long chatId, String text, int wordsCount) {
        Map<String, String> buttons = new LinkedHashMap<>();

        int i = newsListTopSearchCounter.get(chatId) + topSearchOffset + 1;

        if (wordsCount > topSearchOffset) {
            buttons.put("BEFORE_TOP", "« «");
            buttons.put("TOTAL_TOP", (int) Math.ceil((double) (i - 1) / topSearchOffset) + delimiterPages +
                    (int) Math.ceil((double) wordsCount / topSearchOffset));
            buttons.put("NEXT_TOP", "» »");
        }

        if (!text.isBlank()) {
            Integer messageId = sendMessage(chatId, text, InlineKeyboards.maker(buttons));
            newsListTopSearchMessageId.put(chatId, messageId);
            return null;
        }

        return InlineKeyboards.maker(buttons);
    }

    // Одна кнопка для обновления Топа
    private void topKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Кнопки со словами для быстрого удаления
    public void topDeleteKeyboard(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : topData.get(chatId)) {
            s = s.substring(0, s.indexOf(" "));

            if (x <= 4)
                buttons1.put("TOP_DEL_" + x++, s);
            else if (x <= 8)
                buttons2.put("TOP_DEL_" + x++, s);
            else if (x <= 12)
                buttons3.put("TOP_DEL_" + x++, s);
            else if (x <= 16)
                buttons4.put("TOP_DEL_" + x++, s);
            else if (x <= 20)
                buttons5.put("TOP_DEL_" + x++, s);
        }

        sendMessage(chatId, chooseWordDelFromTop, InlineKeyboards.maker(buttons1, buttons2, buttons3, buttons4, buttons5));
    }

    // Кнопки для выбора периода поиска
    public void topPeriodsKeyboard(long chatId) {
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.maker(Buttons.getTopPeriod()));
    }

    // Кнопки для просмотра списка удалённых слов из Топ по страницам
    private InlineKeyboardMarkup getPaginationKeyboard(long chatId, String text, int wordsCount) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("DELETE_TOP", delText);
        buttons1.put("DEL_FROM_TOP", addText);
        buttons1.put("GET_TOP", updateTopText2);

        int i = usersTopPage.get(chatId) + offset + 1;

        if (wordsCount > offset) {
            buttons2.put("FIRST_TOP_PAGE", "««");
            buttons2.put("BEFORE_TOP_PAGE", "«");
            buttons2.put("TOP_PAGE", (int) Math.ceil((double) (i - 1) / offset) + delimiterPages +
                    (int) Math.ceil((double) wordsCount / offset));
            buttons2.put("NEXT_TOP_PAGE", "»");
            buttons2.put("LAST_TOP_PAGE", "»»");
        }

        if (!text.isBlank()) {
            sendMessage(chatId, text, InlineKeyboards.maker(buttons1, buttons2, null, null, null));
            return null;
        }
        return InlineKeyboards.maker(buttons1, buttons2, null, null, null);
    }

    // Включение и выключение применения механизма Джаро-Винклера
    public void showOnOffJaroWinklerKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("JARO_WINKLER_OFF", "Off");
        buttons.put("JARO_WINKLER_ON", "On");

        String isActiveJw = settingsRepository.getJaroWinklerByChatId(chatId);
        String lang = settingsRepository.getLangByChatId(chatId);
        isActiveJw = setOnOffRus(isActiveJw, lang);

        sendMessage(chatId, jaroWinklerSwitcherText + "<b>" + isActiveJw +
                "</b>", InlineKeyboards.maker(buttons));
    }


    /* SETTINGS */
    // Получение списка настроек пользователя
    private void getSettings(long chatId) {
        Optional<Settings> sets = settingsRepository.findById(chatId).stream().findFirst();
        String lang = settingsRepository.getLangByChatId(chatId);
        int isPremium = userRepository.isPremiumByChatId(chatId);

        sets.ifPresentOrElse(x -> {
                    x.setScheduler(Common.setOnOffRus(x.getScheduler(), lang));
                    x.setExcluded(setOnOffRus(x.getExcluded(), lang));

                    String text = getSettingsText(x, lang, isPremium);
                    settingsKeyboard(chatId, text);
                },
                () -> sendMessage(chatId, settingsNotFoundText)
        );
    }

    // Добавление настроек пользователя по умолчанию при его создании
    private void addSettingsByDefault(long chatId) {
        Settings settings = new Settings();
        settings.setChatId(chatId);
        settings.setPeriod("4h");
        settings.setPeriodAll("2h");
        settings.setPeriodTop("12h");
        settings.setScheduler("on");
        settings.setStart(LocalTime.of(10, 0));
        settings.setExcluded("on");
        settings.setLang("en");
        settings.setJaroWinkler("off");
        settings.setPremiumSearch("on");
        settings.setMessageTheme(1);
        settingsRepository.save(settings);
    }

    // Кнопки для раздела настроек (/settings)
    private void settingsKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText2);
        buttons.put("START_SEARCH", "» » »");
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }


    /* DIFFERENT */
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    // Добавление пользователя, запись настроек по умолчанию, установка языка интерфейса (/start)
    private void startActions(Update update, long chatId, String telegramLang) {
        addUser(update.getMessage());

        String chooseNewsLangQuestion = "Select news language!";
        if (telegramLang != null && telegramLang.equals("ru")) {
            chooseNewsLangQuestion = "Выберите язык новостей";
        }
        showLanguagesKeyboard(chatId, chooseNewsLangQuestion);

        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
    }

    // Выбор языка новостей
    public void showLanguagesKeyboard(long chatId, String text) {
        sendMessage(chatId, text, InlineKeyboards.maker(Buttons.getLanguages()));
    }

    // Предложение пользователю продолжить работу в боте на старте
    public void showYesNoOnStartKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("NO_BUTTON", noText);
        buttons.put("YES_BUTTON", yesText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    public void showYesNoGetPremium(long chatId) {
        String premiumExpire = dateFormat(userRepository.findPremiumExpDateByChatId(chatId));

        if (userRepository.isPremiumByChatId(chatId) == 1) {
            sendMessage(chatId, String.format(premiumIsActive2, premiumExpire));
        } else {
            Map<String, String> buttons = new LinkedHashMap<>();
            buttons.put("NO_PREMIUM", noText);
            buttons.put("YES_PREMIUM", yesText);
            sendMessage(chatId, getPremiumYesOrNowText, InlineKeyboards.maker(buttons));
        }
    }

    public void sendPaymentPremium(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("PAYMENT", sendPaymentText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Сохранение пользователя в БД
    private void addUser(Message message) {
        Chat chat = message.getChat();
        User user = new User();
        user.setChatId(message.getChatId());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setUserName(chat.getUserName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
        user.setIsActive(1);
        user.setIsPremium(0);
        user.setPremExpDate(LocalDate.of(2020, 1, 1));

        if (userRepository.isUserExistsByChatId(message.getChatId()) == 0) {
            userRepository.save(user);
            log.info(">>>  Добавлен новый пользователь: {}, {}, {}", user.getChatId(), user.getFirstName(), user.getUserName());
            addSettingsByDefault(message.getChatId());
        } else if (userRepository.isActive(message.getChatId()) == 0) {
            userRepository.updateIsActive(1, message.getChatId());
            log.info(">>> Пользователь снова активен: {}, {}, {}", user.getChatId(), user.getFirstName(), user.getUserName());
        }
    }

    // Установка языка интерфейса и создание меню
    private void setLangAndMenuCreate(long chatId, String lang) {
        setInterfaceLanguage(lang);
        settingsRepository.updateLanguage(lang, chatId);
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
        sendMessage(chatId, greetingText);
        showYesNoOnStartKeyboard(chatId, letsStartText);
        createMenuCommands();
    }

    // Удаление пользователя из БД
    private void deleteUser(long chatId) {
        log.warn("# User delete account: {}", chatId + ", " + userRepository.findNameByChatId(chatId));
        userRepository.deleteById(chatId);
        String text = buyButtonText;
        sendMessage(chatId, text);
    }

    // Подтверждение удаления пользователя (/delete)
    public void showYesNoOnDeleteUser(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_YES", yesText);
        buttons.put("DELETE_NO", noText);
        sendMessage(chatId, confirmDeletedUserText, InlineKeyboards.maker(buttons));
    }

    // Список источников новостей, исходя из выбранного на старте языка интерфейса
    private String getRssList(long chatId) {
        String lang = settingsRepository.getLangByChatId(chatId);
        List<String> sources = rssRepository.findAllActiveSources(chatId, lang);
        List<String> personalSources = rssRepository.findPersonalSources(chatId);

        StringJoiner joiner = new StringJoiner("\n");
        for (String source : sources) {
            joiner.add("  <code>" + source + "</code>");
        }
        String list = rssSourcesText + "\n" + joiner;

        if (personalSources.size() > 0) {
            StringJoiner joinerPers = new StringJoiner("\n");
            for (String source : personalSources) {
                joinerPers.add("  <code>" + source + "</code>");
            }
            list = list + "\n\n" + "<b>" + personalSourcesText + "</b>\n" + joinerPers;
        }

        return list;
    }

    // Кнопки всех видов поиска (/search)
    private void initSearchesKeyboard(long chatId) {
        String delimiterNews = "\n- - - - - - - - - - - - - - - - - - - - - - - -\n";

        String fullText = "<b>1. " +
                searchWithFilterText +
                settingsRepository.getFullSearchPeriod(chatId) + "</b>\n" +
                intervalText3 + " /interval_full\n" +
                listExcludedText +  "  /list_ex\n" +
                cleanFullSearchHistoryText + "  /clear" +
                delimiterNews +
                "<b>" + excludeCategoryText + "</b>\n/sport\n/celebrity\n/negative\n/policy";
        String topText = "<b>2. " + top20Text2 + settingsRepository.getTopPeriod(chatId) + "</b>\n" +
                intervalText3 + " /interval_top\n" +
                excludedListText2 + "  /list_top";
        String keywordsText = "<b>3. " + keywordSearchText + settingsRepository.getKeywordsPeriod(chatId) + "</b>\n" +
                intervalText3 + " /interval\n" +
                listKeywordsButtonText + "    /list_key";


        String text = "<b>" + searchNewsHeaderText + "</b> " + Common.ICON_SEARCH +
                delimiterNews +
                fullText +
                delimiterNews +
                topText +
                delimiterNews +
                keywordsText+
                delimiterNews +
                messageThemeChooseText2 + "<b>/theme</b>";

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FIND_ALL", fullSearchText);
        buttons.put("GET_TOP", updateTopText2);
        buttons.put("FIND_BY_KEYWORDS", keywordsSearchText);

        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Отправка сообщения стандартная
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null && textToSend.length() != 0) message.setText(textToSend);
        else return;
        message.enableHtml(true);
        message.setParseMode(ParseMode.HTML);
        message.disableWebPagePreview();
        executeMessage(message);
    }

    // Отправка сообщения с дополнительной клавиатурой где три вида поиска на старте
    private Integer sendMessage(long chatId, String textToSend, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null && textToSend.length() != 0) message.setText(textToSend);
        else return -1;
        message.enableHtml(true);
        message.setParseMode(ParseMode.HTML);
        message.disableWebPagePreview();
        if (keyboard != null) message.setReplyMarkup(keyboard);
        return executeMessage(message);
    }

    // Отправка сообщения с включённым превью, т.е. если в сообщении есть ссылка, то покажет обложку новости
    private void sendMessageWithPreview(long chatId, String textToSend, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null && textToSend.length() != 0) message.setText(textToSend);
        else return;
        message.enableHtml(true);
        message.setParseMode(ParseMode.HTML);
        if (keyboard != null) message.setReplyMarkup(keyboard);
        executeMessage(message);
    }

    // Механизм отправки сообщений
    private Integer executeMessage(SendMessage message) {
        try {
            Message execute = execute(message);
            return execute.getMessageId();
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("bot was blocked by the user")) {
                userRepository.updateIsActive(0, Long.parseLong(message.getChatId()));
                log.info(String.format("Пользователь chat_id: %s заблокировал бота", message.getChatId()));
            } else {
                log.error(e.getMessage() + String.format("[executeMessage, chat_id: %s]", message.getChatId()));
            }
        }
        return null;
    }

    // Отправка сообщения разработчику
    private void sendFeedback(long chatIdFrom, String text) {
        String userName = userRepository.findNameByChatId(chatIdFrom);
        sendMessage(OWNER_ID, "Message from " + userName + ", " + chatIdFrom + ":\n" + text);
    }

    // Отправка сообщения разработчику с приложением скриншота как документа
    private void sendFeedbackWithDocument(Update update) {
        long chatIdFrom = update.getMessage().getChatId();
        String userName = userRepository.findNameByChatId(chatIdFrom);
        String caption = update.getMessage().getCaption();
        if (caption == null) caption = "не удосужился..";
        List<PhotoSize> photos = update.getMessage().getPhoto();

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(OWNER_ID);
        sendPhoto.setCaption("Message from " + userName + ", " + chatIdFrom + ":\n" + caption);

        List<PhotoSize> photo = update.getMessage().getPhoto();
        if (photo == null) return;
        try {
            String fileId = Objects.requireNonNull(photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null)).getFileId();

            URL url = new URL("https://api.telegram.org/bot" + TOKEN + "/getFile?file_id=" + fileId);

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine();
            String filePath = new JSONObject(res).getJSONObject("result").getString("file_path");
            String urlPhoto = "https://api.telegram.org/file/bot" + TOKEN + "/" + filePath;

            URL url2 = new URL(urlPhoto);
            BufferedImage img = ImageIO.read(url2);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            sendPhoto.setPhoto(new InputFile(is, caption));

            execute(sendPhoto);

        } catch (IOException | TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    // Кнопка отмены ввода слов
    private void cancelKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("CANCEL", cancelButtonText);
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Кнопка показа всех видов поиска
    private void nextKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText2);
        buttons.put("START_SEARCH", "» » »");
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Кнопка показа всех видов поиска
    private void savedKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText2);
        buttons.put("START_SEARCH", "» » »");
        sendMessage(chatId, text, InlineKeyboards.maker(buttons));
    }

    // Кнопки раздела Инфо (/info)
    private void infoKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", sendIdeaText);
        buttons.put("GET_PREMIUM", getPremiumText);
        sendMessageWithPreview(chatId, aboutDeveloperText,
                InlineKeyboards.maker(buttons));
    }

    // Кнопки раздела источники новостей (/sources)
    private void sourcesKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DEL_RSS", delSourceText);
        buttons.put("ADD_RSS", addSourceText);
        sendMessageWithPreview(chatId, getRssList(chatId),
                InlineKeyboards.maker(buttons));
    }

    // После ввода пользователем несуществующей команды предложить добавить слова или выбрать поиск
    void undefinedKeyboard(long chatId, String messageText) {
        if (checkUserInput(chatId, messageText)) return;
        String word = messageText.trim().toLowerCase();

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();

        buttons1.put("FEEDBACK", sendIdeaText2);
        buttons2.put("ADD_KEYWORD_FROM_CHAT", saveText2);
        buttons3.put("SEARCH_BY_WORD", findNewsText2);
        oneWordFromChat.put(chatId, word);

        sendMessageWithPreview(chatId, undefinedCommandText,
                InlineKeyboards.maker(buttons1, buttons2, buttons3, null, null));
    }

    void setThemeKeyboard(long chatId) {
        sendMessageWithPreview(chatId, String.format(messageThemeChooseText,
                settingsRepository.getMessageTheme(chatId)), InlineKeyboards.maker(Buttons.getThemes()));
    }

    // Дополнительная клавиатура с тремя видами поиска (с приветствием пользователя)
    private void getReplyKeyboard(long chatId, String textToSend) {
        String text = String.format(textToSend, "");

        KeyboardRow row = new KeyboardRow();
        row.add(fullSearchText2);
        row.add(updateTopText2);
        row.add(keywordsSearchText);
        sendMessage(chatId, text, ReplyKeyboards.replyKeyboardMaker(row));
    }

    // Текст новостей для пагинации
    private StringJoiner getHeadlinesText(List<Headline> headlines, int current, int next, int counterParts, int offset, long chatId) {
        StringJoiner joiner = new StringJoiner(getDelimiterNews(chatId));
        if (headlines.size() > 0) {
            for (Headline headline : headlines.subList(current, next)) {
                joiner.add(getHeadlinesText(chatId, headline));

                if (counterParts++ > offset) break;
            }
        }
        return joiner;
    }

    // Изменяемое сообщение для пагинации
    private void getEditMessage(Long chatId, int messageId, StringJoiner joiner, InlineKeyboardMarkup keyboard) {
        EditMessageText editOptions = new EditMessageText();
        editOptions.setChatId(chatId);
        editOptions.setMessageId(messageId);
        editOptions.setText(String.valueOf(joiner));
        editOptions.enableHtml(true);
        editOptions.disableWebPagePreview();
        editOptions.setReplyMarkup(keyboard);

        try {
            execute(editOptions);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("message is not modified")) {
                log.debug("[400] Bad Request: message is not modified");
            } else {
                log.error(e.getMessage());
            }
        }
    }

    // Разделитель для сообщений в зависимости от выбранной темы
    private String getDelimiterNews(long chatId) {
        String delimiterNews;
        int messageTheme = settingsRepository.getMessageTheme(chatId);
        if (messageTheme == 2) {
            delimiterNews = "\n\n";
        } else if (messageTheme == 3) {
            delimiterNews = "\n- - - - - - - - - - - - - - - - - - - - - - - -\n";
        } else if (messageTheme == 4) {
            delimiterNews = "\n\n";
        } else if (messageTheme == 5) {
            delimiterNews = "\n";
        } else {
            delimiterNews = "\n- - - - - - - - - - - - - - - - - - - - - - - -\n";
        }
        return delimiterNews;
    }

    // Форматированная новость для показа в чате
    private String getHeadlinesText(long chatId, Headline headline) {
        int messageTheme = settingsRepository.getMessageTheme(chatId);

        if (messageTheme == 2) {
            return String.format("<a href=\"%s\">%s</a>\n<code>%s [%s]</code>",
                    headline.getLink(), headline.getTitle(),
                    headline.getSource(), Common.showDate(String.valueOf(headline.getPubDate())));
        } else if (messageTheme == 3) {
            return String.format("<strong>%s</strong> <a href=\"%s\">»»</a>", headline.getTitle(), headline.getLink());
        } else if (messageTheme == 4) {
            return String.format("<code>%s</code> <a href=\"%s\">»»</a>", headline.getTitle(), headline.getLink());
        } else if (messageTheme == 5) {
            String time = Common.showDate(String.valueOf(headline.getPubDate()));
            time = time.substring(0, time.indexOf(" "));

            return String.format("<strong>%s</strong> <a href=\"%s\">»»</a>\n<pre>%s [%s]</pre>",
                    headline.getTitle(), headline.getLink(), headline.getSource(), time);
        }

        // default theme = 1
        return String.format("<b>%s</b> [%s]\n%s <a href=\"%s\">»»</a>",
                headline.getSource(), Common.showDate(String.valueOf(headline.getPubDate())), headline.getTitle(),
                headline.getLink());
    }

    // Показать статус выполнения операции
    private void showStatus(long chatId, List<String> messages) {
        if (isDone(messages)) {
            for (String message : messages) {
                if (message.equals(DONE)) continue;
                sendMessage(chatId, message);
            }

        } else {
            for (String message : messages) {
                sendMessage(chatId, message);
            }
        }
    }

    // Создание кнопок меню
    private void createMenuCommands() {
        List<BotCommand> commands = new LinkedList<>();
        commands.add(new BotCommand("/settings", settingText));
        commands.add(new BotCommand("/excluding", listExcludedText));
        commands.add(new BotCommand("/keywords", listKeywordsButtonText));
        commands.add(new BotCommand("/search", findSelectText));
        commands.add(new BotCommand("/sources", rssSourcesText2));
        commands.add(new BotCommand("/info", infoText));
        commands.add(new BotCommand("/start", startText));
        commands.add(new BotCommand("/delete", deleteUserText));

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
    }

}