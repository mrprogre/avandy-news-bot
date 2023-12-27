package com.avandy.bot.service;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.InlineKeyboards;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.avandy.bot.utils.Text.*;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    private Long chatIdCallback;
    private static final int TOP_TEN_SHOW_LIMIT = 20;
    private static final int TOP_TEN_LIST_LIMIT = 60;
    private static final int EXCLUDED_LIMIT = 100;
    private static final String REPLACE_ALL_TOP = ",:«»\"";
    private static final String TOP_TEXT = "Top 20";
    private final BotConfig config;
    private Search search;
    private UserRepository userRepository;
    private KeywordRepository keywordRepository;
    private SettingsRepository settingsRepository;
    private ExcludedRepository excludedRepository;
    private RssRepository rssRepository;
    private TopTenRepository topTenRepository;
    private final AtomicBoolean isAutoSearch = new AtomicBoolean(false);
    private StringBuilder stringBuilder;
    private StringJoiner joinerKeywords;
    private String prefix = "";

    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config) {
        super(botToken);
        this.config = config;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setConsoleSearch(Search search) {
        this.search = search;
    }

    @Autowired
    public void setKeywordRepository(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    @Autowired
    public void setSettingsRepository(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Autowired
    public void setExcludedRepository(ExcludedRepository excludedRepository) {
        this.excludedRepository = excludedRepository;
    }

    @Autowired
    public void setRssRepository(RssRepository rssRepository) {
        this.rssRepository = rssRepository;
    }

    @Autowired
    public void setTopTenRepository(TopTenRepository topTenRepository) {
        this.topTenRepository = topTenRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = prefix + update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));

            String firstName = update.getMessage().getChat().getFirstName();
            log.warn(firstName + ": [" + messageText + "]");

            if (messageText.startsWith("/send-feedback")) {
                String feedback = messageText.substring(messageText.indexOf(" ") + 1);
                new Thread(() -> sendFeedback(chatId, feedback)).start();

                /* SEND TO ALL FROM BOT OWNER */
            } else if (messageText.startsWith(":") && config.getBotOwner() == chatId) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                Iterable<User> users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
            } else if (messageText.startsWith("/add-keywords")) {
                String keywords = parseMessageText(messageText);
                String[] words = keywords.split(",");
                addKeyword(chatId, words);
                getKeywordsList(chatId);

            } else if (messageText.startsWith("/add-excluded")) {
                String exclude = parseMessageText(messageText);
                String[] words = exclude.split(",");
                addExclude(chatId, words);
                getExcludedList(chatId);

            } else if (messageText.startsWith("/remove-excluded")) {
                String excluded = parseMessageText(messageText);
                String[] words = excluded.split(",");
                delExcluded(chatId, words);
                getExcludedList(chatId);

            } else if (messageText.startsWith("/remove-keywords")) {
                ArrayList<String> words = new ArrayList<>();
                String keywords = parseMessageText(messageText);
                try {
                    if (keywords.equals("*")) {
                        words.add("*");
                        delKeyword(chatId, words);
                        getKeywordsList(chatId);
                    } else {
                        String[] nums = keywords.split(",");
                        String[] split = joinerKeywords.toString().split("\n");

                        for (String num : nums) {
                            int numInt = Integer.parseInt(num.trim());

                            for (String row : split) {
                                int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                                row = row.replaceAll("\\s", "");
                                row = row.substring(row.indexOf(".") + 1);

                                if (numInt == rowNum) {
                                    words.add(row);
                                }
                            }
                        }
                        delKeyword(chatId, words);
                        getKeywordsList(chatId);
                    }
                } catch (NumberFormatException n) {
                    sendMessage(chatId, allowCommasAndNumbersText);
                    prefix = "";
                } catch (NullPointerException npe) {
                    sendMessage(chatId, "click /keywords" + TOP_TEN_SHOW_LIMIT);
                    prefix = "";
                }

            } else if (messageText.startsWith("/remove-top-ten")) {
                String text = parseMessageText(messageText);
                String[] words = text.split(",");
                delFromTopTenList(chatId, words);
                getTopTenWordsList(chatId);

            } else if (messageText.startsWith("/update-start")) {
                int start = Integer.parseInt(prepareTextToSave(messageText).replaceAll("\\D+", ""));
                try {

                    if (start < 0 || start > 23) {
                        sendMessage(chatId, incorrectTimeText);
                        prefix = "/update-start ";
                    } else {
                        settingsRepository.updateStart(LocalTime.of(start, 0, 0), chatId);
                        sendMessage(chatId, startTimeChangedText);
                        getSettings(chatId);
                        prefix = "";
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            } else if (messageText.startsWith(keywordsSearchText)) {
                new Thread(() -> findNewsByKeywords(chatId)).start();

            } else if (messageText.startsWith(TOP_TEXT)) {
                new Thread(() -> showTop(chatId)).start();

            } else if (messageText.startsWith(fullSearchText)) {
                new Thread(() -> findAllNews(chatId)).start();
            } else {
                /* Команды без параметров */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId);
                    case "/settings" -> new Thread(() -> getSettings(chatId)).start();
                    case "/info" -> new Thread(() -> infoButtons(chatId)).start();
                    case "/search" -> initSearch(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/keywords" -> getKeywordsList(chatId);
                    case "/top" -> showTop(chatId);
                    case "/excluding" -> getExcludedList(chatId);
                    default -> sendMessage(chatId, undefinedCommandText);
                }
            }
            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            chatIdCallback = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "START_SEARCH", "DELETE_NO" -> initSearch(chatIdCallback);

                /* FULL SEARCH */
                case "FIND_ALL" -> new Thread(() -> findAllNews(chatIdCallback)).start();

                /* EXCLUDED */
                case "LIST_EXCLUDED" -> new Thread(() -> getExcludedList(chatIdCallback)).start();
                case "EXCLUDE" -> {
                    prefix = "/add-excluded ";
                    cancelButton(chatIdCallback, addInListText);
                }
                case "DELETE_EXCLUDED" -> {
                    prefix = "/remove-excluded ";
                    cancelButton(chatIdCallback, delFromListText + "\n* - " + removeAllText);
                }

                /* KEYWORDS */
                case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywords(chatIdCallback)).start();
                case "LIST_KEYWORDS" -> new Thread(() -> getKeywordsList(chatIdCallback)).start();
                case "ADD" -> {
                    prefix = "/add-keywords ";
                    cancelButton(chatIdCallback, addInListText);
                }
                case "DELETE" -> {
                    prefix = "/remove-keywords ";
                    cancelButton(chatIdCallback, delFromListText + "\n* - " + removeAllText);
                }

                /* SETTINGS */
                case "GET_SETTINGS" -> new Thread(() -> getSettings(chatIdCallback)).start();
                case "SET_PERIOD" -> getNumbersForKeywordsPeriodButtons(chatIdCallback);
                case "SET_PERIOD_ALL" -> getNumbersForAllPeriodButtons(chatIdCallback);
                case "SET_PERIOD_TOP" -> getNumbersForTopPeriodButtons(chatIdCallback);

                case "SET_SCHEDULER" -> showOnOffScheduler(chatIdCallback);
                case "SCHEDULER_ON" -> {
                    settingsRepository.updateScheduler("on", chatIdCallback);
                    sendMessage(chatIdCallback, schedulerChangedText);
                    getSettings(chatIdCallback);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatIdCallback);
                    sendMessage(chatIdCallback, schedulerChangedText);
                    getSettings(chatIdCallback);
                }
                case "SCHEDULER_START" -> {
                    prefix = "/update-start ";
                    cancelButton(chatIdCallback, inputSchedulerStart);
                }
                case "SET_EXCLUDED" -> showOnOffExcluded(chatIdCallback);
                case "EXCLUDED_ON" -> {
                    settingsRepository.updateExcluded("on", chatIdCallback);
                    sendMessage(chatIdCallback, excludedChangedText);
                    getSettings(chatIdCallback);
                }
                case "EXCLUDED_OFF" -> {
                    settingsRepository.updateExcluded("off", chatIdCallback);
                    sendMessage(chatIdCallback, excludedChangedText);
                    getSettings(chatIdCallback);
                }

                case "FEEDBACK" -> {
                    prefix = "/send-feedback ";
                    cancelButton(chatIdCallback, sendMessageForDevText);
                }

                case "CANCEL" -> {
                    prefix = "";
                    nextButton(chatIdCallback, actionCanceledText);
                }

                case "RU_BUTTON" -> {
                    setLang(chatIdCallback, "ru");
                    setInterfaceLanguage(settingsRepository.getLangByChatId(chatIdCallback));
                    getReplyKeywordWithSearch(chatIdCallback, greetingText, userRepository.findNameByChatId(chatIdCallback));
                    showYesNoOnStart(chatIdCallback, letsStartText);
                    createMenuCommands();
                }
                case "EN_BUTTON" -> {
                    setLang(chatIdCallback, "en");
                    setInterfaceLanguage(settingsRepository.getLangByChatId(chatIdCallback));
                    getReplyKeywordWithSearch(chatIdCallback, greetingText, userRepository.findNameByChatId(chatIdCallback));
                    showYesNoOnStart(chatIdCallback, letsStartText);
                    createMenuCommands();
                }

                // Обновление периода поиска по ключевым словам
                case "BUTTON_1" -> updatePeriod(1, chatIdCallback);
                case "BUTTON_2" -> updatePeriod(2, chatIdCallback);
                case "BUTTON_4" -> updatePeriod(4, chatIdCallback);
                case "BUTTON_12" -> updatePeriod(12, chatIdCallback);
                case "BUTTON_24" -> updatePeriod(24, chatIdCallback);
                case "BUTTON_48" -> updatePeriod(48, chatIdCallback);
                case "BUTTON_72" -> updatePeriod(72, chatIdCallback);

                // Обновление периода поиска по всем новостям
                case "BUTTON_1_ALL" -> updatePeriodAll(1, chatIdCallback);
                case "BUTTON_2_ALL" -> updatePeriodAll(2, chatIdCallback);
                case "BUTTON_4_ALL" -> updatePeriodAll(4, chatIdCallback);
                case "BUTTON_6_ALL" -> updatePeriodAll(6, chatIdCallback);
                case "BUTTON_8_ALL" -> updatePeriodAll(8, chatIdCallback);
                case "BUTTON_12_ALL" -> updatePeriodAll(12, chatIdCallback);
                case "BUTTON_24_ALL" -> updatePeriodAll(24, chatIdCallback);

                case "YES_BUTTON" -> showAddKeywordsButton(chatIdCallback, yesButtonText);
                case "NO_BUTTON" -> sendMessage(chatIdCallback, buyButtonText);
                case "DELETE_YES" -> removeUser(chatIdCallback);

                case "DEL_FROM_TOP" -> deleteFromTopButtons(chatIdCallback);
                case "LIST_TOP" -> getTopTenWordsList(chatIdCallback);
                case "GET_TOP" -> showTop(chatIdCallback);
                case "WORD_SEARCH" -> topSearchButtons(chatIdCallback);
                case "DELETE_TOP" -> {
                    prefix = "/remove-top-ten ";
                    cancelButton(chatIdCallback, removeFromTopTenListText);
                }

                case "TOP_NUM_1" -> searchNewsTop(1, chatIdCallback);
                case "TOP_NUM_2" -> searchNewsTop(2, chatIdCallback);
                case "TOP_NUM_3" -> searchNewsTop(3, chatIdCallback);
                case "TOP_NUM_4" -> searchNewsTop(4, chatIdCallback);
                case "TOP_NUM_5" -> searchNewsTop(5, chatIdCallback);
                case "TOP_NUM_6" -> searchNewsTop(6, chatIdCallback);
                case "TOP_NUM_7" -> searchNewsTop(7, chatIdCallback);
                case "TOP_NUM_8" -> searchNewsTop(8, chatIdCallback);
                case "TOP_NUM_9" -> searchNewsTop(9, chatIdCallback);
                case "TOP_NUM_10" -> searchNewsTop(10, chatIdCallback);
                case "TOP_NUM_11" -> searchNewsTop(11, chatIdCallback);
                case "TOP_NUM_12" -> searchNewsTop(12, chatIdCallback);
                case "TOP_NUM_13" -> searchNewsTop(13, chatIdCallback);
                case "TOP_NUM_14" -> searchNewsTop(14, chatIdCallback);
                case "TOP_NUM_15" -> searchNewsTop(15, chatIdCallback);
                case "TOP_NUM_16" -> searchNewsTop(16, chatIdCallback);
                case "TOP_NUM_17" -> searchNewsTop(17, chatIdCallback);
                case "TOP_NUM_18" -> searchNewsTop(18, chatIdCallback);
                case "TOP_NUM_19" -> searchNewsTop(19, chatIdCallback);
                case "TOP_NUM_20" -> searchNewsTop(20, chatIdCallback);

                case "TOP_DEL_1" -> deleteWordFromTop(1, chatIdCallback);
                case "TOP_DEL_2" -> deleteWordFromTop(2, chatIdCallback);
                case "TOP_DEL_3" -> deleteWordFromTop(3, chatIdCallback);
                case "TOP_DEL_4" -> deleteWordFromTop(4, chatIdCallback);
                case "TOP_DEL_5" -> deleteWordFromTop(5, chatIdCallback);
                case "TOP_DEL_6" -> deleteWordFromTop(6, chatIdCallback);
                case "TOP_DEL_7" -> deleteWordFromTop(7, chatIdCallback);
                case "TOP_DEL_8" -> deleteWordFromTop(8, chatIdCallback);
                case "TOP_DEL_9" -> deleteWordFromTop(9, chatIdCallback);
                case "TOP_DEL_10" -> deleteWordFromTop(10, chatIdCallback);
                case "TOP_DEL_11" -> deleteWordFromTop(11, chatIdCallback);
                case "TOP_DEL_12" -> deleteWordFromTop(12, chatIdCallback);
                case "TOP_DEL_13" -> deleteWordFromTop(13, chatIdCallback);
                case "TOP_DEL_14" -> deleteWordFromTop(14, chatIdCallback);
                case "TOP_DEL_15" -> deleteWordFromTop(15, chatIdCallback);
                case "TOP_DEL_16" -> deleteWordFromTop(16, chatIdCallback);
                case "TOP_DEL_17" -> deleteWordFromTop(17, chatIdCallback);
                case "TOP_DEL_18" -> deleteWordFromTop(18, chatIdCallback);
                case "TOP_DEL_19" -> deleteWordFromTop(19, chatIdCallback);
                case "TOP_DEL_20" -> deleteWordFromTop(20, chatIdCallback);

                case "TOP_INTERVAL_1" -> updatePeriodTop(1, chatIdCallback);
                case "TOP_INTERVAL_4" -> updatePeriodTop(4, chatIdCallback);
                case "TOP_INTERVAL_8" -> updatePeriodTop(8, chatIdCallback);
                case "TOP_INTERVAL_12" -> updatePeriodTop(12, chatIdCallback);
                case "TOP_INTERVAL_24" -> updatePeriodTop(24, chatIdCallback);
                case "TOP_INTERVAL_48" -> updatePeriodTop(48, chatIdCallback);
                case "TOP_INTERVAL_72" -> updatePeriodTop(72, chatIdCallback);
            }
        }
    }

    private void deleteWordFromTop(int wordNum, long chatId) {
        String word = "empty";

        try {
            String[] split = stringBuilder.toString().split("\n");

            for (String row : split) {
                int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                row = row.replaceAll("\\s", "");
                row = row.substring(row.indexOf(".") + 1, row.indexOf("["));

                if (wordNum == rowNum) {
                    word = row;
                }
            }

            addTopTen(chatId, word);
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
    }

    private void searchNewsTop(int wordNum, long chatId) {
        String word = "empty";

        try {
            String[] split = stringBuilder.toString().split("\n");

            for (String row : split) {
                int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                row = row.replaceAll("\\s", "");
                row = row.substring(row.indexOf(".") + 1, row.indexOf("["));

                if (wordNum == rowNum) {
                    word = row;
                }
            }

            wordSearch(chatId, word);
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
    }

    private void updatePeriod(int period, long chatId) {
        settingsRepository.updatePeriod(period + "h", chatId);
        sendMessage(chatId, changeIntervalText);
        initSearch(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, changeIntervalText);
        initSearch(chatId);
    }

    private void updatePeriodTop(int period, long chatId) {
        settingsRepository.updatePeriodTop(period + "h", chatId);
        sendMessage(chatId, changeIntervalText);
        showTop(chatId);
    }

    private void removeUser(long chatId) {
        log.warn("User {} deleted account", userRepository.findById(chatId));
        userRepository.deleteById(chatId);
        String text = buyButtonText;
        sendMessage(chatId, text);
    }

    private String getRssList() {
        Iterable<RssList> sources = rssRepository.findAllActiveSources();

        StringJoiner joiner = new StringJoiner(", ");
        for (RssList item : sources) {
            joiner.add(item.getSource());
        }

        return "<b>" + rssSourcesText + "</b>\n" + joiner;
    }

    private void startActions(Update update, long chatId) {
        addUser(update.getMessage());
        showLangButtons(chatId, "Choose your language");
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
    }

    private static String prepareTextToSave(String messageText) {
        return messageText.substring(messageText.indexOf(" ")).trim().toLowerCase();
    }

    private void initSearch(long chatId) {
        String keywordsText = "1. " + keywordSearchText + "\n[" + settingsRepository.getPeriodByChatId(chatId) + ", " +
                keywordRepository.getKeywordsCountByChatId(chatId) + " " + keywordSearch2Text + "]";
        String fullText = "2. " + searchWithFilterText + "\n[" + settingsRepository.getPeriodAllByChatId(chatId) + ", " +
                excludedRepository.getExcludedCountByChatId(chatId) + " " + searchWithFilter2Text + "]";

        SendMessage message = prepareMessage(chatId, keywordsText + "\n- - - - - -\n" + fullText);
        message.enableHtml(true);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("SET_PERIOD", intervalText);
        buttons1.put("FIND_BY_KEYWORDS", listKeywordsText);
        buttons2.put("SET_PERIOD_ALL", intervalText);
        buttons2.put("FIND_ALL", fullSearchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, null, null, null));
        executeMessage(message);
    }

    private void getSettings(long chatId) {
        Optional<Settings> sets = settingsRepository.findById(chatId).stream().findFirst();
        String lang = settingsRepository.getLangByChatId(chatId);

        sets.ifPresentOrElse(x -> {
                    String text = getSettingsText(x, lang);
                    getSettingsButtons(chatId, text);
                },
                () -> sendMessage(chatId, settingsNotFoundText)
        );
    }

    private void getKeywordsList(long chatId) {
        int counter = 0;
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (!keywordsByChatId.isEmpty()) {
            joinerKeywords = new StringJoiner("\n");
            for (String item : keywordsByChatId) {
                joinerKeywords.add(++counter + ". " + item);
            }

            showKeywordButtons(chatId, "<b>" + listKeywordsText + "</b>\n" + joinerKeywords);
        } else {
            showAddKeywordsButton(chatId, setupKeywordsText);
        }
    }

    private void getExcludedList(long chatId) {
        List<String> excludedByChatId = excludedRepository.findExcludedByChatId(chatId);
        int excludedCount = excludedByChatId.size();

        if (excludedByChatId.size() >= 400) {
            excludedByChatId = excludedRepository.findExcludedByChatIdLimit(chatId, EXCLUDED_LIMIT);
        }

        if (!excludedByChatId.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (String item : excludedByChatId) {
                joiner.add(item);
            }
            showExcludedButtons(chatId, "<b>" + exclusionWordsText + "</b> [" + excludedCount + "]\n" +
                    joiner);
        } else {
            showExcludeButton(chatId);
        }
    }

    private String parseMessageText(String messageText) {
        return messageText.substring(messageText.indexOf(" "))
                .trim()
                .toLowerCase();
    }

    public SendMessage prepareMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        return message;
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = prepareMessage(chatId, textToSend);
        message.enableHtml(true);
        message.disableWebPagePreview();
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("bot was blocked by the user")) {
                userRepository.updateIsActive(0, Long.parseLong(message.getChatId()));
                log.warn(String.format("Пользователь chat_id: %s, т.к. заблокировал бота", message.getChatId()));
            } else {
                log.warn(e.getMessage() + String.format("[chat_id: %s]", message.getChatId()));
            }
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    private void addUser(Message message) {
        Chat chat = message.getChat();
        User user = new User();
        user.setChatId(message.getChatId());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setUserName(chat.getUserName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
        user.setIsActive(1);

        if (userRepository.isUserExistsByChatId(message.getChatId()) == 0) {
            userRepository.save(user);
            log.warn("* * * * * * ДОБАВЛЕН НОВЫЙ ПОЛЬЗОВАТЕЛЬ: {} * * * * * *", user);
            addSettings(message.getChatId());
        } else if (userRepository.isActive(message.getChatId()) == 0) {
            userRepository.updateIsActive(1, message.getChatId());
            log.warn("* * * * * * ПОЛЬЗОВАТЕЛЬ СНОВА АКТИВЕН: {} * * * * * *", user);
        }
    }

    private void addTopTen(long chatId, String word) {
        Set<String> topTenWordsByChatId = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        TopTenExcluded topTenExcluded;
        if (!topTenWordsByChatId.contains(word)) {
            topTenExcluded = new TopTenExcluded();
            topTenExcluded.setUserId(chatId);
            topTenExcluded.setWord(word);

            try {
                topTenRepository.save(topTenExcluded);
                sendMessage(chatId, "❌ " + word);
            } catch (Exception e) {
                if (e.getMessage().contains("ui_top_ten_excluded")) {
                    log.info(wordIsExistsText + word);
                }
            }

        } else {
            sendMessage(chatId, wordIsExistsText + word);
        }
    }

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
        prefix = "";
    }

    private void addExclude(long chatId, String[] list) {
        int counter = 0;
        List<String> excludedByChatId = excludedRepository.findExcludedByChatId(chatId);

        Excluded excluded;
        for (String word : list) {
            word = word.trim().toLowerCase();
            if (!(word.length() <= 2)) {
                if (!excludedByChatId.contains(word)) {
                    excluded = new Excluded();
                    excluded.setChatId(chatId);
                    excluded.setWord(word);

                    try {
                        excludedRepository.save(excluded);
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

        prefix = "";
    }

    private void delKeyword(long chatId, ArrayList<String> keywords) {
        for (String word : keywords) {
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
        prefix = "";
    }

    private void delExcluded(long chatId, String[] excluded) {
        for (String exclude : excluded) {
            exclude = exclude.trim().toLowerCase();

            if (exclude.equals("*")) {
                excludedRepository.deleteAllExcludedByChatId(chatId);
                sendMessage(chatId, deleteAllWordsText);
                break;
            }

            if (excludedRepository.isWordExists(chatId, exclude) > 0) {
                excludedRepository.deleteExcludedByChatId(chatId, exclude);
                sendMessage(chatId, "Удалено слово - " + exclude + " ❌");
            } else {
                sendMessage(chatId, "Слово " + exclude + " отсутствует в списке");
            }
        }
        prefix = "";
    }

    private void addSettings(long chatId) {
        Settings settings = new Settings();
        settings.setChatId(chatId);
        settings.setPeriod("1h");
        settings.setPeriodAll("1h");
        settings.setPeriodTop("12h");
        settings.setScheduler("on");
        settings.setStart(LocalTime.of(10, 0));
        settings.setExcluded("on");
        settings.setLang("ru");
        settingsRepository.save(settings);
        prefix = "";
    }

    private void findAllNews(long chatId) {
        sendMessage(chatId, fullSearchStartText);
        Set<Headline> headlines = search.start(chatId, "all");

        int counterParts = 1;
        int showAllCounter = 1;
        if (headlines.size() > 0) {
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

            String text = foundNewsText + " <b>" + Search.totalNewsCounter + "</b> (" + excludedNewsText +
                    " <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>)";

            nextButtonAfterAllSearch(chatId, text);
        } else {
            String text = EmojiParser.parseToUnicode(headlinesNotFound);
            nextButtonAfterAllSearch(chatId, text);
        }
    }

    private int findNewsByKeywords(long chatId) {
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (isAutoSearch.get() && keywordsByChatId.isEmpty()) {
            return 0;
        }

        if (!isAutoSearch.get() && keywordsByChatId.isEmpty()) {
            showAddKeywordsButton(chatId, setupKeywordsText);
            return 0;
        }

        if (!isAutoSearch.get()) {
            sendMessage(chatId, searchByKeywordsStartText);
        }

        // Search
        Set<Headline> headlines = search.start(chatId, "keywords");

        int counterParts = 1;
        int showCounter = 1;
        if (headlines.size() > 0) {
            StringJoiner joiner = new StringJoiner("\n- - - - - -\n");
            for (Headline headline : headlines) {
                String text = "<b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                if (isAutoSearch.get()) {
                    joiner.add(text);
                } else {
                    joiner.add(showCounter++ + ". " + text);
                }

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

            String text = foundNewsText + ": <b>" + Search.filteredNewsCounter + "</b>";

            if (!isAutoSearch.get()) {
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        } else {
            if (!isAutoSearch.get()) {
                String text = EmojiParser.parseToUnicode(headlinesNotFound);
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        }
        return Search.filteredNewsCounter;
    }


    private void wordSearch(long chatId, String word) {
        Set<Headline> headlines = search.start(chatId, word);

        int counterParts = 1;
        int showCounter = 1;
        if (headlines.size() > 0) {
            StringJoiner joiner = new StringJoiner("\n- - - - - -\n");
            for (Headline headline : headlines) {
                joiner.add(showCounter++ + ". <b>" + headline.getSource() + "</b> [" +
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

            String text = foundNewsText + ": <b>" + Search.filteredNewsCounter + "</b>";

            showTopTenButton(chatId, text);
        } else {
            String text = EmojiParser.parseToUnicode(headlinesNotFound);
            showTopTenButton(chatId, text);
        }
    }

    private void sendFeedback(long chatId, String text) {
        String userName = userRepository.findNameByChatId(chatId);
        sendMessage(1254981379, "<b>Message</b> from user: " + userName + ", id: " + chatId + "\n" + text);
        prefix = "";
    }

    public void showOnOffScheduler(long chatId) {
        SendMessage message = prepareMessage(chatId, autoSearchText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showOnOffExcluded(long chatId) {
        SendMessage message = prepareMessage(chatId, exclusionText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDED_OFF", "Off");
        buttons.put("EXCLUDED_ON", "On");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showYesNoOnStart(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("NO_BUTTON", noText);
        buttons.put("YES_BUTTON", yesText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showLangButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("RU_BUTTON", "rus");
        buttons.put("EN_BUTTON", "eng");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showYesNoOnDeleteUser(long chatId) {
        SendMessage message = prepareMessage(chatId, confirmDeletedUserText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_YES", yesText);
        buttons.put("DELETE_NO", noText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void getNumbersForKeywordsPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseSearchDepthText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1", "1");
        buttons.put("BUTTON_2", "2");
        buttons.put("BUTTON_4", "4");
        buttons.put("BUTTON_12", "12");
        buttons.put("BUTTON_24", "24");
        buttons.put("BUTTON_48", "48");
        buttons.put("BUTTON_72", "72");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void getNumbersForAllPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseSearchDepthText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("BUTTON_1_ALL", "1");
        buttons.put("BUTTON_2_ALL", "2");
        buttons.put("BUTTON_4_ALL", "4");
        buttons.put("BUTTON_6_ALL", "6");
        buttons.put("BUTTON_8_ALL", "8");
        buttons.put("BUTTON_12_ALL", "12");
        buttons.put("BUTTON_24_ALL", "24");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void getNumbersForTopPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseSearchDepthText);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TOP_INTERVAL_1", "1");
        buttons.put("TOP_INTERVAL_4", "4");
        buttons.put("TOP_INTERVAL_8", "8");
        buttons.put("TOP_INTERVAL_12", "12");
        buttons.put("TOP_INTERVAL_24", "24");
        buttons.put("TOP_INTERVAL_48", "48");
        buttons.put("TOP_INTERVAL_72", "72");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void topSearchButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseNumberWordFromTop);
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : getTopTen(chatId)) {
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

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
        executeMessage(message);
    }

    public void deleteFromTopButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseWordDelFromTop);
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        Map<String, String> buttons4 = new LinkedHashMap<>();
        Map<String, String> buttons5 = new LinkedHashMap<>();

        int x = 1;
        for (String s : getTopTen(chatId)) {
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

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
        executeMessage(message);
    }

    private void cancelButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("CANCEL", cancelButtonText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showExcludedButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("DELETE_EXCLUDED", delText);
        buttons1.put("EXCLUDE", addText);
        buttons2.put("SET_PERIOD_ALL", intervalText);
        buttons2.put("FIND_ALL", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, null, null, null));
        executeMessage(message);
    }

    private void showExcludeButton(long chatId) {
        SendMessage message = prepareMessage(chatId, excludedWordsNotSetText);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", addText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("DELETE", delText);
        buttons1.put("ADD", addText);
        buttons2.put("SET_PERIOD", intervalText);
        buttons2.put("FIND_BY_KEYWORDS", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, null, null, null));
        executeMessage(message);
    }

    private void showAddKeywordsButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD", addText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getSettingsButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);
        boolean isOn = settingsRepository.getSchedulerOnOffByChatId(chatId).equals("on");

        Map<String, String> buttons = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        buttons.put("SET_PERIOD", "1. " + intervalText);
        buttons.put("SET_PERIOD_ALL", "2. " + intervalText);
        buttons2.put("SET_SCHEDULER", "3. " + autoSearchText);
        buttons2.put("SET_EXCLUDED", "4. " + exclusionText);

        if (isOn) {
            buttons3.put("SCHEDULER_START", "5. " + startSettingsText);
            buttons3.put("START_SEARCH", "» » »");
        } else {
            buttons3.put("START_SEARCH", "» » »");
        }

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons, buttons2, buttons3, null, null));
        executeMessage(message);
    }

    private void nextButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("START_SEARCH", "» » »");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void infoButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, aboutDeveloperText + "\n\n" + getRssList());
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", sendIdeaText);
        buttons.put("START_SEARCH", "» » »");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterKeywordsSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD", intervalText);
        buttons.put("LIST_KEYWORDS", listKeywordsText);
        buttons.put("FIND_BY_KEYWORDS", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD_ALL", intervalText);
        buttons.put("EXCLUDE", excludeWordText);
        buttons.put("FIND_ALL", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    // Показать топ слов за период
    private void showTopTenButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();

        buttons1.put("LIST_TOP", excludedListText);
        buttons1.put("DEL_FROM_TOP", delFromTopText);
        buttons2.put("SET_PERIOD_TOP", intervalText);
        buttons2.put("GET_TOP", updateTopText);
        buttons3.put("WORD_SEARCH", searchByTopWordText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
        executeMessage(message);
    }

    private void showTopTenButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText);
        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    // Список удалённых слов из топа
    private void showTopTenListButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_TOP", delText);
        buttons.put("DEL_FROM_TOP", addText);
        buttons.put("GET_TOP", TOP_TEXT);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getTopTenWordsList(long chatId) {
        Set<String> items = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        showTopTenListButtons(chatId, "<b>" + listOfDeletedFromTopText + "</b> [" + items.size() + "]\n" +
                items.stream()
                        .limit(TOP_TEN_LIST_LIMIT)
                        .toList()
                        .toString()
                        .replace("[", "")
                        .replace("]", "")
        );
    }

    private void showTop(long chatId) {
        // init
        int x = 1;
        search.start(chatId, "top");

        List<String> topTen = getTopTen(chatId);
        if (topTen.size() > 0) {
            stringBuilder = new StringBuilder();
            String point = ".    ";

            for (String s : topTen) {
                if (x >= 10) point = ".  ";
                stringBuilder.append(x++).append(point).append(s);
            }

            String period = settingsRepository.getPeriodTopByChatId(chatId);

            showTopTenButtons(chatId,
                    String.format("%s<b>%s</b>]\n%s", top20ByPeriodText, period, stringBuilder));
        } else {
            showTopTenButton(chatId, updateTopText);
        }
    }

    private List<String> getTopTen(long chatId) {
        Map<String, Integer> wordsCount = new HashMap<>();

        for (Headline headline : Search.headlinesTopTen) {
            // Удаление ненужных знаков
            String[] titles = headline.getTitle()
                    .replaceAll(REPLACE_ALL_TOP, "")
                    .toLowerCase()
                    .split(" ");

            for (String word : titles) {
                if (word.length() > 2) {
                    wordsCount.put(word, wordsCount.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Удаление исключённых слов из мап для анализа
        Set<String> excludedWordsFromAnalysis = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);
        for (String word : excludedWordsFromAnalysis) {
            wordsCount.remove(word);
        }

        return wordsCount.entrySet().stream()
                .filter(x -> x.getValue() > 3)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(TOP_TEN_SHOW_LIMIT)
                .map(x -> String.format("%s [<b>%d</b>]", x.getKey(), x.getValue()) + "\n")
                .toList();
    }

    private void delFromTopTenList(long chatId, String[] words) {
        for (String word : words) {
            word = word.trim().toLowerCase();

            if (topTenRepository.isWordExists(chatId, word) > 0) {
                topTenRepository.deleteWordByChatId(chatId, word);
                sendMessage(chatId, "❌ " + word);
            } else {
                sendMessage(chatId, String.format(wordIsNotInTheListText, word));
            }
        }
        prefix = "";
    }

    private void setLang(long chatId, String lang) {
        setInterfaceLanguage(lang);
        settingsRepository.updateLanguage(lang, chatId);
    }

    @Async
    @Scheduled(cron = "${cron.search.keywords}")
    protected void autoSearchByKeywords() {
        Integer hourNow = LocalTime.now().getHour();
        isAutoSearch.set(true);

        List<Settings> usersSettings = settingsRepository.findAllByScheduler();
        for (Settings setting : usersSettings) {
            List<Integer> timeToExecute = Common.getTimeToExecute(setting.getStart(), setting.getPeriod());
            String username = userRepository.findNameByChatId(setting.getChatId());

            if (timeToExecute.contains(hourNow)) {
                if (userRepository.isActive(setting.getChatId()) == 1) {
                    int counter = findNewsByKeywords(setting.getChatId());

                    if (counter > 0)
                        log.warn("Автопоиск ключевых слов. Пользователь: {}, найдено {}", username, counter);
                }
            }
        }
        isAutoSearch.set(false);
    }

    // Нижняя клавиатура
    private void getReplyKeywordWithSearch(long chatId, String textToSend, String firstName) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(String.format(textToSend, firstName));

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(fullSearchText);
        row.add(keywordsSearchText);
        row.add(TOP_TEXT);

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void createMenuCommands() {
        List<BotCommand> listOfCommands = new LinkedList<>();
        listOfCommands.add(new BotCommand("/settings", settingText));
        listOfCommands.add(new BotCommand("/keywords", listKeywordsText));
        listOfCommands.add(new BotCommand("/top", top20Text));
        listOfCommands.add(new BotCommand("/search", findSelectText));
        listOfCommands.add(new BotCommand("/excluding", listExcludedText));
        listOfCommands.add(new BotCommand("/info", infoText));
        //listOfCommands.add(new BotCommand("/rss", listRssText));
        //listOfCommands.add(new BotCommand("/delete", deleteUserText));
        //listOfCommands.add(new BotCommand("/start", startText));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
    }
}