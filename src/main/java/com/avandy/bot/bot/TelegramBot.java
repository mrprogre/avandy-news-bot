package com.avandy.bot.bot;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.search.Search;
import com.avandy.bot.search.SearchService;
import com.avandy.bot.utils.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, StringBuilder> usersTop = new ConcurrentHashMap<>();
    private final SearchService searchService;
    private final BotConfig config;
    private final RssRepository rssRepository;
    private final UserRepository userRepository;
    private final TopTenRepository topTenRepository;
    private final KeywordRepository keywordRepository;
    private final SettingsRepository settingsRepository;
    private final ExcludingTermsRepository excludingTermsRepository;

    @Autowired
    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config,
                       SearchService searchService, UserRepository userRepository, KeywordRepository keywordRepository,
                       SettingsRepository settingsRepository, ExcludingTermsRepository excludingTermsRepository,
                       RssRepository rssRepository, TopTenRepository topTenRepository) {
        super(botToken);
        this.config = config;
        this.searchService = searchService;
        this.userRepository = userRepository;
        this.keywordRepository = keywordRepository;
        this.settingsRepository = settingsRepository;
        this.excludingTermsRepository = excludingTermsRepository;
        this.rssRepository = rssRepository;
        this.topTenRepository = topTenRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверка наличия не пустого сообщения
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userTelegramLanguageCode = update.getMessage().getFrom().getLanguageCode();

            setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
            UserState userState = userStates.get(chatId);

            // DEBUG: запись действий пользователя для анализа
            if (Common.DEV_ID != chatId) {
                if (userState != null) log.warn("{}, {}: message: {}", chatId, userRepository.findNameByChatId(chatId),
                        userState + " " + messageText);
                else log.warn("{}, {}: message: {}", chatId, userRepository.findNameByChatId(chatId), messageText);
            }

            // SEND TO ALL FROM BOT OWNER
            if (messageText.startsWith(":") && config.getBotOwner() == chatId) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                List<User> users = userRepository.findAllByIsActive();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
                // SEND TO CHAT_ID FROM BOT OWNER
            } else if (messageText.startsWith("@") && config.getBotOwner() == chatId) {
                long chatToSend = Long.parseLong(messageText.substring(1, messageText.indexOf(" ")));
                String textToSend = messageText.substring(messageText.indexOf(" "));
                sendMessage(chatToSend, textToSend);

                /* USER INPUT BLOCK */
            } else if (UserState.SEND_FEEDBACK.equals(userState)) {
                String feedback = messageText.substring(messageText.indexOf(" ") + 1);
                sendFeedback(chatId, feedback);

            } else if (UserState.ADD_KEYWORDS.equals(userState)) {
                String keywords = messageText.trim().toLowerCase();
                String[] words = keywords.split(",");
                addKeyword(chatId, words);
                showKeywordsList(chatId);

            } else if (UserState.DEL_KEYWORDS.equals(userState)) {
                String keywords = messageText.trim().toLowerCase();
                deleteKeywords(keywords, chatId);

            } else if (UserState.ADD_EXCLUDED.equals(userState)) {
                String exclude = messageText.trim().toLowerCase();
                String[] words = exclude.split(",");
                addExclude(chatId, words);
                getExcludedList(chatId);

            } else if (UserState.DEL_EXCLUDED.equals(userState)) {
                String excluded = messageText.trim().toLowerCase();
                String[] words = excluded.split(",");
                delExcluded(chatId, words);
                getExcludedList(chatId);

            } else if (UserState.DEL_TOP.equals(userState)) {
                String text = messageText.trim().toLowerCase();
                String[] words = text.split(",");
                topListDelete(chatId, words);
                getTopTenWordsList(chatId);

                // Поиск по трём кнопкам меню
            } else if (messageText.equals(keywordsSearchText)) {
                new Thread(() -> findNewsByKeywordsManual(chatId)).start();

            } else if (messageText.equals(updateTopText2)) {
                new Thread(() -> showTop(chatId)).start();

            } else if (messageText.equals(fullSearchText)) {
                new Thread(() -> fullSearch(chatId)).start();
            } else {
                /* Основные команды */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId, userTelegramLanguageCode);
                    case "/settings" -> getSettings(chatId);
                    case "/search" -> initSearchesKeyboard(chatId);
                    case "/info" -> infoKeyboard(chatId);
                    case "/keywords" -> showKeywordsList(chatId);
                    case "/excluding" -> getExcludedList(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/top" -> showTop(chatId);
                    default -> undefinedKeyboard(chatId);
                }
            }
            userStates.remove(chatId);

            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));

            switch (callbackData) {
                case "GET_PREMIUM" -> showYesNoGetPremium(chatId);
                case "YES_PREMIUM" -> {
                    sendMessage(Common.DEV_ID, String.format(">>> Хочет премиум: %d, %s. Проверить оплату!", chatId,
                            userRepository.findNameByChatId(chatId)));
                    sendMessage(chatId, getPremiumRequestText);
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
                    sendMessage(chatId, changesSavedText);
                    getSettings(chatId);
                }
                case "EXCLUDED_OFF" -> {
                    settingsRepository.updateExcluded("off", chatId);
                    sendMessage(chatId, changesSavedText);
                    getSettings(chatId);
                }

                case "JARO_WINKLER_MODE" -> showOnOffJaroWinklerKeyboard(chatId);
                case "JARO_WINKLER_ON" -> {
                    settingsRepository.updateJaroWinkler("on", chatId);
                    sendMessage(chatId, changesSavedText);
                }
                case "JARO_WINKLER_OFF" -> {
                    settingsRepository.updateJaroWinkler("off", chatId);
                    sendMessage(chatId, changesSavedText);
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
                case "BUTTON_12" -> keywordsUpdatePeriod(12, chatId);
                case "BUTTON_24" -> keywordsUpdatePeriod(24, chatId);
                case "BUTTON_48" -> keywordsUpdatePeriod(48, chatId);
                case "BUTTON_72" -> keywordsUpdatePeriod(72, chatId);

                // Обновление периода поиска по всем новостям
                case "BUTTON_1_ALL" -> fullSearchPeriodUpdate(1, chatId);
                case "BUTTON_2_ALL" -> fullSearchPeriodUpdate(2, chatId);
                case "BUTTON_4_ALL" -> fullSearchPeriodUpdate(4, chatId);
                case "BUTTON_6_ALL" -> fullSearchPeriodUpdate(6, chatId);
                case "BUTTON_8_ALL" -> fullSearchPeriodUpdate(8, chatId);
                case "BUTTON_12_ALL" -> fullSearchPeriodUpdate(12, chatId);
                case "BUTTON_24_ALL" -> fullSearchPeriodUpdate(24, chatId);

                case "YES_BUTTON" -> addKeywordsKeyboard(chatId, yesButtonText);
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

                case "TOP_INTERVAL_1" -> topUpdatePeriod(1, chatId);
                case "TOP_INTERVAL_4" -> topUpdatePeriod(4, chatId);
                case "TOP_INTERVAL_8" -> topUpdatePeriod(8, chatId);
                case "TOP_INTERVAL_12" -> topUpdatePeriod(12, chatId);
                case "TOP_INTERVAL_24" -> topUpdatePeriod(24, chatId);
                case "TOP_INTERVAL_48" -> topUpdatePeriod(48, chatId);
                case "TOP_INTERVAL_72" -> topUpdatePeriod(72, chatId);

                /* AUTO SEARCH BY KEYWORDS */
                case "SET_SCHEDULER" -> keywordsOnOffSchedulerKeyboard(chatId);
                case "SCHEDULER_ON" -> {
                    settingsRepository.updateScheduler("on", chatId);
                    sendMessage(chatId, changesSavedText);
                    getSettings(chatId);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatId);
                    sendMessage(chatId, changesSavedText);
                    getSettings(chatId);
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
            }
        }
    }

    /* KEYWORDS */
    // Ручной поиск по ключевым словам
    public void findNewsByKeywordsManual(long chatId) {
        String nameByChatId = userRepository.findNameByChatId(chatId);
        String keywordsPeriod = settingsRepository.getKeywordsPeriod(chatId);

        // DEBUG
        if (Common.DEV_ID != chatId) {
            log.warn("Ручной запуск поиска по ключевым словам: {}, {} за {}", chatId, nameByChatId, keywordsPeriod);
        }

        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (keywordsByChatId.isEmpty()) {
            addKeywordsKeyboard(chatId, setupKeywordsText);
            return;
        }

        getReplyKeyboard(chatId, searchByKeywordsStartText, "");

        // Search
        Set<Headline> headlines = searchService.start(chatId, "keywords");

        int showCounter = 1;
        if (headlines.size() > 0) {
            // INFO
            log.warn("Найдено: {}, {}, новостей {}, глубина {}", chatId, nameByChatId, headlines.size(), keywordsPeriod);

            for (Headline headline : headlines) {
                String text = "<b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                text = showCounter++ + ". " + text;
                sendMessage(chatId, text);
            }

            afterKeywordsSearchKeyboard(chatId,
                    foundNewsText + ": <b>" + headlines.size() + "</b> " + Common.ICON_NEWS_FOUNDED);

        } else {
            afterKeywordsSearchKeyboard(chatId, headlinesNotFound);
        }
    }

    // Автоматический поиск по ключевым словам
    public void autoSearchNewsByKeywords(long chatId) {
        String nameByChatId = userRepository.findNameByChatId(chatId);
        String keywordsPeriod = settingsRepository.getKeywordsPeriod(chatId);
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (keywordsByChatId.isEmpty()) {
            return;
        }

        // Search
        Set<Headline> headlines = searchService.start(chatId, "keywords");

        if (headlines.size() > 0) {
            // INFO
            log.warn("Автопоиск: {}, {}, найдено {} за {}", chatId, nameByChatId, headlines.size(), keywordsPeriod);

            for (Headline headline : headlines) {
                String text = "<b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                sendMessage(chatId, text);
            }
        }
    }

    // Формирование списка ключевых слов в виде строки
    private String getKeywordsList(long chatId) {
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
    private void showKeywordsList(long chatId) {
        String joinerKeywords = getKeywordsList(chatId);

        if (joinerKeywords != null && joinerKeywords.length() != 0) {
            keywordsListKeyboard(chatId, listKeywordsText + "\n" + joinerKeywords);
        } else {
            addKeywordsKeyboard(chatId, setupKeywordsText);
        }
    }

    // Добавление ключевых слов
    private void addKeyword(long chatId, String[] keywords) {
        int counter = 0;
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        Keyword word;
        for (String keyword : keywords) {
            keyword = keyword.trim().toLowerCase();
            if (!(keyword.length() <= 2)) {
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
                    sendMessage(chatId, wordIsExistsText + keyword);
                }
            } else {
                sendMessage(chatId, minWordLengthText);
            }
        }
        if (counter != 0) {
            sendMessage(chatId, wordsAddedText + " - " + counter + " ✔️");
        } else {
            sendMessage(chatId, wordsIsNotAddedText);
        }
    }

    // Изменение периода поиска
    private void keywordsUpdatePeriod(int period, long chatId) {
        settingsRepository.updateKeywordsPeriod(period + "h", chatId);
        sendMessage(chatId, changesSavedText);
        initSearchesKeyboard(chatId);
    }

    // Удаление ключевых слов. Порядковые номера преобразуются в слова, которые потом и удаляются из БД
    private void deleteKeywords(String keywordNumbers, long chatId) {
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
                    sendMessage(chatId, deleteAllWordsText);
                    break;
                }

                if (keywordRepository.isKeywordExists(chatId, word) > 0) {
                    keywordRepository.deleteKeywordByChatId(chatId, word);
                    sendMessage(chatId, "❌ " + word);
                } else {
                    sendMessage(chatId, String.format(wordIsNotInTheListText, word));
                }
            }

            showKeywordsList(chatId);
        } catch (NumberFormatException n) {
            sendMessage(chatId, allowCommasAndNumbersText);
        } catch (NullPointerException npe) {
            sendMessage(chatId, "click /keywords" + Common.TOP_TEN_SHOW_LIMIT);
        }
    }

    // Изменение времени старта запуска автопоиска по ключевым словам
    private void keywordsUpdateSearchStartTime(int start, long chatId) {
        settingsRepository.updateStart(LocalTime.of(start, 0, 0), chatId);
        sendMessage(chatId, changesSavedText);
        getSettings(chatId);
    }

    // KEYBOARDS
    private void addKeywordsKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD_KEYWORD", addText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void keywordsChangePeriodKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1", "1");
        buttons.put("BUTTON_2", "2");
        buttons.put("BUTTON_4", "4");
        buttons.put("BUTTON_12", "12");
        buttons.put("BUTTON_24", "24");
        buttons.put("BUTTON_48", "48");
        buttons.put("BUTTON_72", "72");
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void keywordsListKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_KEYWORD", delText);
        buttons.put("ADD_KEYWORD", addText);
        buttons.put("FIND_BY_KEYWORDS", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки включения и выключения авто запуска автопоиска
    private void keywordsOnOffSchedulerKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");
        sendMessage(chatId, autoSearchText, InlineKeyboards.inlineKeyboardMaker(buttons));
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

        sendMessage(chatId, chooseSearchStartText, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, null));
    }

    // Кнопки после итогов поиска по ключевым словам
    private void afterKeywordsSearchKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD", intervalText);
        buttons.put("LIST_KEYWORDS", listText);
        buttons.put("FIND_BY_KEYWORDS", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }


    /* FULL SEARCH */
    // Полный поиск
    private void fullSearch(long chatId) {
        // DEBUG
        if (Common.DEV_ID != chatId) {
            log.warn("{}: Запуск полного поиска", chatId);
        }

        getReplyKeyboard(chatId, fullSearchStartText, "");
        //sendMessage(chatId, fullSearchStartText);

        Set<Headline> headlines = searchService.start(chatId, "all");

        int counterParts = 1;
        int showAllCounter = 1;
        if (headlines.size() > 0) {

            if (headlines.size() > Common.LIMIT_FOR_BREAKING_INTO_PARTS) {
                // 10 message in 1
                StringJoiner joiner = new StringJoiner("\n- - - - - -\n");
                for (Headline headline : headlines) {

                    joiner.add(showAllCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                            Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                            headline.getTitle() + " " +
                            "<a href=\"" + headline.getLink() + "\">link</a>");

                    if (counterParts == 10) {
                        sendMessage(chatId, String.valueOf(joiner));
                        joiner = new StringJoiner("\n- - - - - -\n");
                        counterParts = 0;
                    }
                    counterParts++;
                }


                if (counterParts != 0) {
                    sendMessage(chatId, String.valueOf(joiner));
                }
            } else {
                for (Headline headline : headlines) {

                    sendMessage(chatId, showAllCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                            Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                            headline.getTitle() + " " +
                            "<a href=\"" + headline.getLink() + "\">link</a>");
                }
            }

            afterFullSearchKeyboard(chatId, foundNewsText + " <b>" + headlines.size() + "</b> (" +
                    excludedNewsText + " <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>) " +
                    Common.ICON_NEWS_FOUNDED);
        } else {
            afterFullSearchKeyboard(chatId, headlinesNotFound);
        }
    }

    // Изменение глубины полного поиска
    private void fullSearchPeriodUpdate(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, changesSavedText);
        initSearchesKeyboard(chatId);
    }

    // KEYBOARDS
    // Кнопки выбора глубины полного поиска
    public void fullSearchPeriodChangeKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1_ALL", "1");
        buttons.put("BUTTON_2_ALL", "2");
        buttons.put("BUTTON_4_ALL", "4");
        buttons.put("BUTTON_6_ALL", "6");
        buttons.put("BUTTON_8_ALL", "8");
        buttons.put("BUTTON_12_ALL", "12");
        buttons.put("BUTTON_24_ALL", "24");
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки после итогов полного поиска
    private void afterFullSearchKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD_ALL", intervalText);
        buttons.put("ADD_EXCLUDED", excludeWordText);
        buttons.put("FIND_ALL", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }


    /* EXCLUDING TERMS */
    // Список слов-исключений (/excluding)
    private void getExcludedList(long chatId) {
        List<String> excludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);
        int excludedCount = excludedByChatId.size();

        if (excludedByChatId.size() >= 400) {
            excludedByChatId = excludingTermsRepository.findExcludedByChatIdLimit(chatId, Common.EXCLUDED_LIMIT);
        }

        if (!excludedByChatId.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            int counter = 0;
            for (String item : excludedByChatId) {
                joiner.add(item);
                if (++counter == Common.EXCLUDING_TERMS_LIST_LIMIT) break;
            }
            excludeListKeyboard(chatId, "<b>" + exclusionWordsText + "</b> [" + excludedCount + "]\n" +
                    joiner);
        } else {
            excludeKeyboard(chatId);
        }
    }

    // Добавление слов-исключений
    private void addExclude(long chatId, String[] list) {
        int counter = 0;
        List<String> excludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);

        ExcludingTerm excluded;
        for (String word : list) {
            word = word.trim().toLowerCase();
            if (!(word.length() <= 2)) {
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
                    sendMessage(chatId, wordIsExistsText + word);
                }
            } else {
                sendMessage(chatId, minWordLengthText);
            }
        }

        if (counter != 0) {
            sendMessage(chatId, addedExceptionWordsText + " - " + counter + " ✔️");
        } else {
            sendMessage(chatId, wordsIsNotAddedText);
        }
    }

    // Удаление слов-исключений
    private void delExcluded(long chatId, String[] excluded) {
        for (String exclude : excluded) {
            exclude = exclude.trim().toLowerCase();

            if (exclude.equals("*")) {
                excludingTermsRepository.deleteAllExcludedByChatId(chatId);
                sendMessage(chatId, deleteAllWordsText);
                break;
            }

            if (excludingTermsRepository.isWordExists(chatId, exclude) > 0) {
                excludingTermsRepository.deleteExcludedByChatId(chatId, exclude);
                sendMessage(chatId, "Удалено слово - " + exclude + " ❌");
            } else {
                sendMessage(chatId, "Слово " + exclude + " отсутствует в списке");
            }
        }
    }

    // KEYBOARDS
    // Кнопки включения и выключения механизма исключения заголовков
    public void excludeOnOffKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDED_OFF", "Off");
        buttons.put("EXCLUDED_ON", "On");
        sendMessage(chatId, exclusionText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки дял списка исключённых слов
    private void excludeListKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_EXCLUDED", delText);
        buttons.put("ADD_EXCLUDED", addText);
        buttons.put("FIND_ALL", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Одна кнопка с добавлением в слова-исключения
    private void excludeKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD_EXCLUDED", addText);
        sendMessage(chatId, excludedWordsNotSetText, InlineKeyboards.inlineKeyboardMaker(buttons));
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
        Set<String> excludedWordsFromAnalysis = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);
        for (String word : excludedWordsFromAnalysis) {
            wordsCount.remove(word);
        }

        return wordsCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(Common.TOP_TEN_SHOW_LIMIT)
                .map(x -> String.format("%s [<b>%d</b>]", x.getKey(), x.getValue()) + "\n")
                .toList();
    }

    // Поиск новостей по слову из Топа
    private void searchNewsTop(int wordNum, long chatId) {
        try {
            String word = getWordByNumber(wordNum, chatId);
            wordSearch(chatId, word);
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
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

    // Механизм поиска по слову из Топа
    private void wordSearch(long chatId, String word) {
        // DEBUG
        if (Common.DEV_ID != chatId) {
            log.warn("{}: Запуск поиска по словам из Топ 20", chatId);
        }

        Set<Headline> headlines = searchService.start(chatId, word);

        int showCounter = 1;
        if (headlines.size() > 0) {
            for (Headline headline : headlines) {
                String text = showCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                sendMessage(chatId, text);
            }

            topSearchKeyboard(chatId);
        } else {
            topKeyboard(chatId, headlinesNotFound);
        }
    }

    // Список удалённых слов из Топа
    private void getTopTenWordsList(long chatId) {
        Set<String> items = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        topDeletedListKeyboard(chatId, "<b>" + listOfDeletedFromTopText + "</b> [" + items.size() + "]\n" +
                items.stream()
                        .limit(Common.TOP_TEN_LIST_LIMIT)
                        .toList()
                        .toString()
                        .replace("[", "")
                        .replace("]", "")
        );
    }

    // Быстрое удаление слова из показа в Топе по кнопке
    private void topDelete(int wordNum, long chatId) {
        try {
            String word = getWordByNumber(wordNum, chatId);
            Set<String> topTenWordsByChatId = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

            TopExcluded topExcluded;
            if (!topTenWordsByChatId.contains(word)) {
                topExcluded = new TopExcluded();
                topExcluded.setChatId(chatId);
                topExcluded.setWord(word);

                try {
                    topTenRepository.save(topExcluded);
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
        sendMessage(chatId, changesSavedText);
        showTop(chatId);
    }

    // Удаление слов из списка удалённых из Топа
    private void topListDelete(long chatId, String[] words) {
        for (String word : words) {
            word = word.trim().toLowerCase();

            if (topTenRepository.isWordExists(chatId, word) > 0) {
                topTenRepository.deleteWordByChatId(chatId, word);
                sendMessage(chatId, "❌ " + word);
            } else {
                sendMessage(chatId, String.format(wordIsNotInTheListText, word));
            }
        }
    }

    // KEYBOARDS
    // Основные кнопки Топа
    private void topActionsKeyboard(long chatId, String text) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();

        buttons1.put("LIST_TOP", excludedListText);
        buttons1.put("DEL_FROM_TOP", delFromTopText);
        buttons2.put("SET_PERIOD_TOP", intervalText);
        buttons2.put("GET_TOP", updateTopText);
        buttons3.put("JARO_WINKLER_MODE", jaroWinklerText);
        buttons3.put("SEARCH_BY_TOP_WORD", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
    }

    // Кнопки со словами для быстрого поиска
    public void topSearchKeyboard(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : topWordsPrepare(chatId)) {
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
        sendMessage(chatId, chooseNumberWordFromTop, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
    }

    // Одна кнопка для обновления Топа
    private void topKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки со словами для быстрого удаления
    public void topDeleteKeyboard(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : topWordsPrepare(chatId)) {
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

        sendMessage(chatId, chooseWordDelFromTop, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
    }

    // Кнопки для выбора периода поиска
    public void topPeriodsKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TOP_INTERVAL_1", "1");
        buttons.put("TOP_INTERVAL_4", "4");
        buttons.put("TOP_INTERVAL_8", "8");
        buttons.put("TOP_INTERVAL_12", "12");
        buttons.put("TOP_INTERVAL_24", "24");
        buttons.put("TOP_INTERVAL_48", "48");
        buttons.put("TOP_INTERVAL_72", "72");
        sendMessage(chatId, chooseSearchDepthText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки для списка удалённых слов из топа
    private void topDeletedListKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_TOP", delText);
        buttons.put("DEL_FROM_TOP", addText);
        buttons.put("GET_TOP", updateTopText2);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Включение и выключение применения механизма Джаро-Винклера
    public void showOnOffJaroWinklerKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("JARO_WINKLER_OFF", "Off");
        buttons.put("JARO_WINKLER_ON", "On");

        String isActiveJw = settingsRepository.getJaroWinklerByChatId(chatId);
        isActiveJw = setOnOffRus(isActiveJw, chatId);

        sendMessage(chatId, jaroWinklerSwitcherText + "<b>" + isActiveJw +
                "</b>", InlineKeyboards.inlineKeyboardMaker(buttons));
    }


    /* SETTINGS */
    // Получение списка настроек пользователя
    private void getSettings(long chatId) {
        Optional<Settings> sets = settingsRepository.findById(chatId).stream().findFirst();
        String lang = settingsRepository.getLangByChatId(chatId);
        int isPremium = userRepository.isPremiumByChatId(chatId);

        sets.ifPresentOrElse(x -> {
                    x.setScheduler(setOnOffRus(x.getScheduler(), chatId));
                    x.setExcluded(setOnOffRus(x.getExcluded(), chatId));

                    String text = getSettingsText(x, lang, isPremium);
                    settingsKeyboard(chatId, text);
                },
                () -> sendMessage(chatId, settingsNotFoundText)
        );
    }

    // Добавление настроек пользователя по умолчанию при его создании
    private void addDefaultSettings(long chatId) {
        Settings settings = new Settings();
        settings.setChatId(chatId);
        settings.setPeriod("12h");
        settings.setPeriodAll("1h");
        settings.setPeriodTop("12h");
        settings.setScheduler("on");
        settings.setStart(LocalTime.of(10, 0));
        settings.setExcluded("on");
        settings.setLang("en");
        settings.setJaroWinkler("on");
        settingsRepository.save(settings);
    }

    // Кнопки для раздела настроек (/settings)
    private void settingsKeyboard(long chatId, String text) {
        boolean isOn = settingsRepository.getSchedulerOnOffByChatId(chatId).equals("on");
        int isPremium = userRepository.isPremiumByChatId(chatId);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        buttons1.put("SET_SCHEDULER", "1. " + autoSearchText);
        buttons1.put("SET_PERIOD", "2. " + intervalText);
        buttons2.put("SET_EXCLUDED", "3. " + exclusionText);
        buttons2.put("SET_PERIOD_ALL", "4. " + intervalText);

        if (isOn && isPremium != 1) {
            buttons3.put("SCHEDULER_START", "5. " + startSettingsText);
            buttons3.put("START_SEARCH", "» » »");
        } else {
            buttons3.put("START_SEARCH", "» » »");
        }

        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
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
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DE_BUTTON", "de");
        buttons.put("FR_BUTTON", "fr");
        buttons.put("ES_BUTTON", "es");
        buttons.put("EN_BUTTON", "en");
        buttons.put("RU_BUTTON", "ru");
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Предложение пользователю продолжить работу в боте на старте
    public void showYesNoOnStartKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("NO_BUTTON", noText);
        buttons.put("YES_BUTTON", yesText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void showYesNoGetPremium(long chatId) {
        String premiumExpire = dateFormat(userRepository.findPremiumExpDateByChatId(chatId));

        if (userRepository.isPremiumByChatId(chatId) == 1) {
            sendMessage(chatId, String.format(premiumIsActive2, premiumExpire));
        } else {
            Map<String, String> buttons = new LinkedHashMap<>();
            buttons.put("NO_PREMIUM", noText);
            buttons.put("YES_PREMIUM", yesText);
            sendMessage(chatId, getPremiumYesOrNowText, InlineKeyboards.inlineKeyboardMaker(buttons));
        }
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
            log.warn(">>>  Добавлен новый пользователь: {}, {}, {}", user.getChatId(), user.getFirstName(),
                    user.getUserName());
            addDefaultSettings(message.getChatId());
        } else if (userRepository.isActive(message.getChatId()) == 0) {
            userRepository.updateIsActive(1, message.getChatId());
            log.warn(">>> Пользователь снова активен: {}, {}, {}", user.getChatId(), user.getFirstName(),
                    user.getUserName());
        }
    }

    // Установка языка интерфейса и создание меню
    private void setLangAndMenuCreate(long chatId, String lang) {
        setInterfaceLanguage(lang);
        settingsRepository.updateLanguage(lang, chatId);
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
        getReplyKeyboard(chatId, greetingText, userRepository.findNameByChatId(chatId));
        showYesNoOnStartKeyboard(chatId, letsStartText);
        createMenuCommands();
    }

    // Удаление пользователя из БД
    private void deleteUser(long chatId) {
        log.warn("User {} deleted account", userRepository.findById(chatId));
        userRepository.deleteById(chatId);
        String text = buyButtonText;
        sendMessage(chatId, text);
    }

    // Подтверждение удаления пользователя (/delete)
    public void showYesNoOnDeleteUser(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_YES", yesText);
        buttons.put("DELETE_NO", noText);
        sendMessage(chatId, confirmDeletedUserText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Список источников новостей, исходя из выбранного на старте языка интерфейса
    private String getRssList(long chatId) {
        String lang = settingsRepository.getLangByChatId(chatId);
        List<String> sources = rssRepository.findAllActiveSources(lang);

        StringJoiner joiner = new StringJoiner(", ");
        for (String source : sources) {
            joiner.add(source);
        }

        return "<b>" + rssSourcesText + "</b>\n" + joiner;
    }

    // Кнопки всех видов поиска (/search)
    private void initSearchesKeyboard(long chatId) {
        String keywordsText = String.format("1" + initSearchTemplateText, keywordSearchText,
                settingsRepository.getKeywordsPeriod(chatId), keywordRepository.getKeywordsCountByChatId(chatId),
                keywordSearch2Text);

        String fullText = String.format("2" + initSearchTemplateText, searchWithFilterText,
                settingsRepository.getFullSearchPeriod(chatId), excludingTermsRepository.getExcludedCountByChatId(chatId),
                searchWithFilter2Text);

        String topText = String.format("3" + initSearchTemplateText, top20Text2,
                settingsRepository.getTopPeriod(chatId), topTenRepository.deleteFromTopTenCount(chatId),
                removedFromTopText);

        String text = "<b>" + searchNewsHeaderText + "</b> " + Common.ICON_SEARCH +
                "\n- - - - - -\n" +
                keywordsText +
                "\n- - - - - -\n" +
                fullText +
                "\n- - - - - -\n" +
                topText +
                "\n- - - - - -";

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();

        // Keywords search
        buttons1.put("SET_PERIOD", intervalText);
        buttons1.put("LIST_KEYWORDS", listText);
        buttons1.put("FIND_BY_KEYWORDS", keywordsSearchText);

        // Full search
        buttons2.put("SET_PERIOD_ALL", intervalText);
        buttons2.put("LIST_EXCLUDED", listText);
        buttons2.put("FIND_ALL", fullSearchText);

        // Top 20
        buttons3.put("SET_PERIOD_TOP", intervalText);
        buttons3.put("LIST_TOP", listText);
        buttons3.put("GET_TOP", updateTopText2);

        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
    }

    // Отправка сообщения стандартная
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableHtml(true);
        message.disableWebPagePreview();
        executeMessage(message);
    }

    // Отправка сообщения с дополнительной клавиатурой где три вида поиска на старте
    private void sendMessage(long chatId, String textToSend, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null) message.setText(textToSend);
        else return;
        message.enableHtml(true);
        message.disableWebPagePreview();
        if (keyboard != null) message.setReplyMarkup(keyboard);
        executeMessage(message);
    }

    // Отправка сообщения с включённым превью, т.е. если в сообщении есть ссылка, то покажет обложку новости
    private void sendMessageWithPreview(long chatId, String textToSend, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null) message.setText(textToSend);
        else return;
        message.enableHtml(true);
        if (keyboard != null) message.setReplyMarkup(keyboard);
        executeMessage(message);
    }

    // Механизм отправки сообщений
    private void executeMessage(SendMessage message) {
        try {
            execute(message);

            // Предупреждение выброса исключения, что много сообщений в секунду
            try {
                Thread.sleep(Common.SLEEP_BETWEEN_SENDING_MESSAGES);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }

        } catch (TelegramApiException e) {
            if (e.getMessage().contains("bot was blocked by the user")) {
                userRepository.updateIsActive(0, Long.parseLong(message.getChatId()));
                log.warn(String.format("Пользователь chat_id: %s, т.к. заблокировал бота", message.getChatId()));
            } else {
                log.warn(e.getMessage() + String.format("[chat_id: %s]", message.getChatId()));
            }
        }
    }

    // Отправка сообщения разработчику
    private void sendFeedback(long chatId, String text) {
        String userName = userRepository.findNameByChatId(chatId);
        sendMessage(1254981379, "<b>Message</b> from user: " + userName + ", id: " + chatId + "\n" + text);
    }

    // Преобразование on/off в русские слова
    private String setOnOffRus(String value, long chatId) {
        String lang = settingsRepository.getLangByChatId(chatId);
        if (lang.equals("ru")) {
            value = value.equals("on") ? "вкл [on]" : "выкл [off]";
        }
        return value;
    }

    // Кнопка отмены ввода слов
    private void cancelKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("CANCEL", cancelButtonText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопка показа всех видов поиска
    private void nextKeyboard(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("START_SEARCH", "» » »");
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Кнопки раздела Инфо (/info)
    private void infoKeyboard(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", sendIdeaText);
        buttons.put("GET_PREMIUM", getPremiumText);
        buttons.put("START_SEARCH", "» » »");
        sendMessageWithPreview(chatId, aboutDeveloperText + "\n\n" + getRssList(chatId),
                InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // После ввода пользователем несуществующей команды предложить добавить слова или выбрать поиск
    void undefinedKeyboard(long chatId) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        buttons1.put("ADD_KEYWORD", addText2);
        buttons2.put("ADD_EXCLUDED", excludeWordText2);
        buttons3.put("START_SEARCH", findSelectText);

        sendMessageWithPreview(chatId, undefinedCommandText,
                InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
    }

    // Дополнительная клавиатура с тремя видами поиска (с приветствием пользователя)
    private void getReplyKeyboard(long chatId, String textToSend, String firstName) {
        String text = textToSend;

        if (firstName != null) {
            text = String.format(textToSend, firstName);
        }

        KeyboardRow row = new KeyboardRow();
        row.add(keywordsSearchText);
        row.add(fullSearchText);
        row.add(updateTopText2);
        sendMessage(chatId, text, ReplyKeyboards.replyKeyboardMaker(row));
    }

    // Создание кнопок меню
    private void createMenuCommands() {
        List<BotCommand> listOfCommands = new LinkedList<>();
        listOfCommands.add(new BotCommand("/settings", settingText));
        listOfCommands.add(new BotCommand("/keywords", listKeywordsButtonText));
        listOfCommands.add(new BotCommand("/search", findSelectText));
        listOfCommands.add(new BotCommand("/info", infoText));
        //listOfCommands.add(new BotCommand("/top", top20Text));
        //listOfCommands.add(new BotCommand("/delete", deleteUserText));
        //listOfCommands.add(new BotCommand("/excluding", listExcludedText));
        //listOfCommands.add(new BotCommand("/start", startText));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
    }

}