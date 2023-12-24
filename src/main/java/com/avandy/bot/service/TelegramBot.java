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
    public static final int TOP_TEN_SHOW_LIMIT = 20;
    public static final int TOP_TEN_LIST_LIMIT = 60;
    public static final int EXCLUDED_LIMIT = 100;
    private final BotConfig config;
    private Search search;
    private UserRepository userRepository;
    private KeywordRepository keywordRepository;
    private SettingsRepository settingsRepository;
    private ExcludedRepository excludedRepository;
    private RssRepository rssRepository;
    private TodoRepository todoRepository;
    private TopTenRepository topTenRepository;
    public static AtomicBoolean isAutoSearch = new AtomicBoolean(false);
    private static StringBuilder stringBuilder;
    static String prefix = "";
    static String topText = "Top 20";


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

    @Autowired
    public void setTodoRepository(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
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
                sendFeedback(chatId, feedback);
                prefix = "";

                // Send to all from bot owner
            } else if (messageText.startsWith(":") && config.getBotOwner() == chatId) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                Iterable<User> users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
            } else if (messageText.equals("/tasks")) {
                getTodoList(chatId);

            } else if (messageText.startsWith("/addtodo") && messageText.length() > 8 && messageText.charAt(8) == ' ') {
                addTodo(messageText, chatId);
                getTodoList(chatId);
                prefix = "";

            } else if (messageText.startsWith("/deltodo") && messageText.length() > 8 && messageText.charAt(8) == ' ') {
                deleteTodo(messageText, chatId);
                getTodoList(chatId);
                prefix = "";

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

            } else if (messageText.startsWith("/addtop") && messageText.length() > 7 && messageText.charAt(7) == ' ') {
                ArrayList<String> words = new ArrayList<>();
                String tops = parseMessageText(messageText);
                String[] nums = tops.split(",");

                try {
                    String[] split = stringBuilder.toString().split("\n");

                    for (String num : nums) {
                        int numInt = Integer.parseInt(num);

                        for (String row : split) {
                            int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                            row = row.replaceAll("\\s", "");
                            row = row.substring(row.indexOf(".") + 1, row.indexOf("["));

                            if (numInt == rowNum) {
                                words.add(row);
                            }
                        }
                    }
                    addTopTen(chatId, words);
                    showTopTen(chatId);
                } catch (NumberFormatException n) {
                    sendMessage(chatId, allowCommasAndNumbersText);
                    prefix = "";
                } catch (NullPointerException npe) {
                    sendMessage(chatId, startSearchBeforeText);
                    prefix = "";
                }

            } else if (messageText.startsWith("/findtop") && messageText.length() > 8 && messageText.charAt(8) == ' ') {
                int num = Integer.parseInt(prepareTextToSave(messageText).replaceAll("\\D+", ""));
                showNewsByWordNumberForTop20(num, chatId);

            } else if (messageText.startsWith("/remove-excluded")) {
                String keywords = parseMessageText(messageText);
                String[] words = keywords.split(",");
                delExcluded(chatId, words);
                getExcludedList(chatId);

            } else if (messageText.startsWith("/remove-keywords")) {
                String keywords = parseMessageText(messageText);
                String[] words = keywords.split(",");
                delKeyword(chatId, words);
                getKeywordsList(chatId);

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

            } else if (messageText.startsWith(topText)) {
                showTopTen(chatId);

            } else if (messageText.startsWith(fullSearchText)) {
                new Thread(() -> findAllNews(chatId)).start();
            } else {
                /* Команды без параметров */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId);
                    case "/settings" -> new Thread(() -> getSettings(chatId)).start();
                    case "/info" -> new Thread(() -> infoButtons(chatId)).start();
                    case "/search" -> initSearchButtons(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/keywords" -> getKeywordsList(chatId);
                    case "/top" -> showTopTen(chatId);
                    case "/rss" -> getRssList(chatId);
                    case "/excluded" -> getExcludedList(chatId);
                    default -> sendMessage(chatId, undefinedCommandText);
                }
            }
            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "START_SEARCH", "DELETE_NO" -> initSearchButtons(chatId);

                /* FULL SEARCH */
                case "FIND_ALL" -> new Thread(() -> findAllNews(chatId)).start();

                /* EXCLUDED */
                case "LIST_EXCLUDED" -> new Thread(() -> getExcludedList(chatId)).start();
                case "EXCLUDE" -> {
                    prefix = "/add-excluded ";
                    cancelButton(chatId, addInListText);
                }
                case "DELETE_EXCLUDED" -> {
                    prefix = "/remove-excluded ";
                    cancelButton(chatId, delFromListText + "\n* - " + removeAllText);
                }

                /* KEYWORDS */
                case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywords(chatId)).start();
                case "LIST_KEYWORDS" -> new Thread(() -> getKeywordsList(chatId)).start();
                case "ADD" -> {
                    prefix = "/add-keywords ";
                    cancelButton(chatId, addInListText);
                }
                case "DELETE" -> {
                    prefix = "/remove-keywords ";
                    cancelButton(chatId, delFromListText + "\n* - " + removeAllText);
                }

                /* SETTINGS */
                case "GET_SETTINGS" -> getSettings(chatId);
                case "SET_PERIOD" -> getNumbersForKeywordsPeriodButtons(chatId);
                case "SET_PERIOD_ALL" -> getNumbersForAllPeriodButtons(chatId);
                case "SET_SCHEDULER" -> showOnOffScheduler(chatId);
                case "SCHEDULER_ON" -> {
                    settingsRepository.updateScheduler("on", chatId);
                    sendMessage(chatId, schedulerChangedText);
                    getSettings(chatId);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatId);
                    sendMessage(chatId, schedulerChangedText);
                    getSettings(chatId);
                }
                case "SCHEDULER_START" -> {
                    prefix = "/update-start ";
                    cancelButton(chatId, inputSchedulerStart);
                }
                case "SET_EXCLUDED" -> showOnOffExcluded(chatId);
                case "EXCLUDED_ON" -> {
                    settingsRepository.updateExcluded("on", chatId);
                    sendMessage(chatId, excludedChangedText);
                    getSettings(chatId);
                }
                case "EXCLUDED_OFF" -> {
                    settingsRepository.updateExcluded("off", chatId);
                    sendMessage(chatId, excludedChangedText);
                    getSettings(chatId);
                }

                case "TODO_LIST" -> getTodoList(chatId);
                case "TODO_ADD" -> {
                    prefix = "/addtodo ";
                    cancelButton(chatId, addTodoText);
                }
                case "TODO_DEL" -> {
                    prefix = "/deltodo ";
                    cancelButton(chatId, delTodoText);
                }

                case "FEEDBACK" -> {
                    prefix = "/send-feedback ";
                    cancelButton(chatId, sendMessageForDevText);
                }

                case "CANCEL" -> {
                    prefix = "";
                    nextButton(chatId, actionCanceledText);
                }

                case "RU_BUTTON" -> {
                    setLang(chatId, "ru");
                    setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
                    getReplyKeywordWithSearch(chatId, greetingText, userRepository.findNameByChatId(chatId));
                    showYesNoOnStart(chatId, letsStartText);
                    createMenuCommands();
                }
                case "EN_BUTTON" -> {
                    setLang(chatId, "en");
                    setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
                    getReplyKeywordWithSearch(chatId, greetingText, userRepository.findNameByChatId(chatId));
                    showYesNoOnStart(chatId, letsStartText);
                    createMenuCommands();
                }

                // Обновление периода поиска по ключевым словам
                case "BUTTON_1" -> updatePeriod(1, chatId);
                case "BUTTON_2" -> updatePeriod(2, chatId);
                case "BUTTON_4" -> updatePeriod(4, chatId);
                case "BUTTON_12" -> updatePeriod(12, chatId);
                case "BUTTON_24" -> updatePeriod(24, chatId);
                case "BUTTON_48" -> updatePeriod(48, chatId);
                case "BUTTON_72" -> updatePeriod(72, chatId);

                // Обновление периода поиска по всем новостям
                case "BUTTON_1_ALL" -> updatePeriodAll(1, chatId);
                case "BUTTON_2_ALL" -> updatePeriodAll(2, chatId);
                case "BUTTON_4_ALL" -> updatePeriodAll(4, chatId);
                case "BUTTON_6_ALL" -> updatePeriodAll(6, chatId);
                case "BUTTON_8_ALL" -> updatePeriodAll(8, chatId);
                case "BUTTON_12_ALL" -> updatePeriodAll(12, chatId);
                case "BUTTON_24_ALL" -> updatePeriodAll(24, chatId);

                case "YES_BUTTON" -> showAddKeywordsButton(chatId, yesButtonText);
                case "NO_BUTTON" -> sendMessage(chatId, buyButtonText);
                case "DELETE_YES" -> removeUser(chatId);

                case "ADD_TOP" -> {
                    prefix = "/addtop ";
                    cancelButton(chatId, delFromTopInstText);
                }
                case "LIST_TOP" -> getTopTenWordsList(chatId);
                case "GET_TOP" -> showTopTen(chatId);
                case "WORD_SEARCH" -> getNumberTopSearchByWord(chatId);
                case "DELETE_TOP" -> {
                    prefix = "/remove-top-ten ";
                    cancelButton(chatId, removeFromTopTenListText);
                }

                case "TOP_NUM_1" -> showNewsByWordNumberForTop20(1, chatId);
                case "TOP_NUM_2" -> showNewsByWordNumberForTop20(2, chatId);
                case "TOP_NUM_3" -> showNewsByWordNumberForTop20(3, chatId);
                case "TOP_NUM_4" -> showNewsByWordNumberForTop20(4, chatId);
                case "TOP_NUM_5" -> showNewsByWordNumberForTop20(5, chatId);
                case "TOP_NUM_6" -> showNewsByWordNumberForTop20(6, chatId);
                case "TOP_NUM_7" -> showNewsByWordNumberForTop20(7, chatId);
                case "TOP_NUM_8" -> showNewsByWordNumberForTop20(8, chatId);
                case "TOP_NUM_9" -> showNewsByWordNumberForTop20(9, chatId);
                case "TOP_NUM_10" -> showNewsByWordNumberForTop20(10, chatId);
                case "TOP_NUM_11" -> showNewsByWordNumberForTop20(11, chatId);
                case "TOP_NUM_12" -> showNewsByWordNumberForTop20(12, chatId);
                case "TOP_NUM_13" -> showNewsByWordNumberForTop20(13, chatId);
                case "TOP_NUM_14" -> showNewsByWordNumberForTop20(14, chatId);
                case "TOP_NUM_15" -> showNewsByWordNumberForTop20(15, chatId);
                case "TOP_NUM_16" -> showNewsByWordNumberForTop20(16, chatId);
                case "TOP_NUM_17" -> showNewsByWordNumberForTop20(17, chatId);
                case "TOP_NUM_18" -> showNewsByWordNumberForTop20(18, chatId);
                case "TOP_NUM_19" -> showNewsByWordNumberForTop20(19, chatId);
                case "TOP_NUM_20" -> showNewsByWordNumberForTop20(20, chatId);
            }
        }
    }

    private void showNewsByWordNumberForTop20(int wordNum, long chatId) {
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
        } catch (NumberFormatException n) {
            sendMessage(chatId, allowNumberText);
        } catch (NullPointerException npe) {
            sendMessage(chatId, startSearchBeforeText);
        }
    }

    private void createMenuCommands() {
        List<BotCommand> listOfCommands = new LinkedList<>();
        listOfCommands.add(new BotCommand("/settings", settingText));
        listOfCommands.add(new BotCommand("/excluded", listExcludedText));
        listOfCommands.add(new BotCommand("/keywords", listKeywordsText));
        listOfCommands.add(new BotCommand("/search", findSelectText));
        listOfCommands.add(new BotCommand("/top", top20Text));
        listOfCommands.add(new BotCommand("/rss", listRssText));
        listOfCommands.add(new BotCommand("/tasks", listTodoText));
        listOfCommands.add(new BotCommand("/info", infoText));
        listOfCommands.add(new BotCommand("/delete", deleteUserText));
        listOfCommands.add(new BotCommand("/start", startText));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
    }

    private void updatePeriod(int period, long chatId) {
        settingsRepository.updatePeriod(period + "h", chatId);
        sendMessage(chatId, changeIntervalText);
        getSettings(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, changeIntervalText);
        getSettings(chatId);
    }

    private void deleteTodo(String messageText, long chatId) {
        String toDel = parseMessageText(messageText);
        String[] ids = toDel.split(",");

        for (String id : ids) {
            int idToDel = Integer.parseInt(id);

            if (todoRepository.findById(idToDel).isPresent()) {
                todoRepository.deleteById(idToDel);
            }
        }
        sendMessage(chatId, taskDeletedText);
    }

    private void addTodo(String messageText, long chatId) {
        String[] texts = messageText
                .substring(messageText.indexOf(" "))
                .split(";");

        for (String text : texts) {
            todoRepository.save(new Todo(chatId, text.trim()));
        }
        sendMessage(chatId, taskAddedText);
    }

    private void getTodoList(long chatId) {
        List<Todo> todoList = todoRepository.findByChatId(chatId);

        if (todoList.size() > 0) {
            StringJoiner joiner = new StringJoiner("\n");
            for (Todo item : todoList) {
                joiner.add(item.getId() + ". " + item.getText() + " [" + item.getAddDate() + "]");
            }
            showTodoButtons(chatId, "<b>" + taskListText + "</b>\n" + joiner);

        } else {
            showTodoAddButton(chatId);
        }
    }

    private void removeUser(long chatId) {
        log.warn("User {} deleted account", userRepository.findById(chatId));
        userRepository.deleteById(chatId);
        String text = buyButtonText;
        sendMessage(chatId, text);
    }

    private void getRssList(long chatId) {
        Iterable<RssList> sources = rssRepository.findAllActiveSources();

        StringJoiner joiner = new StringJoiner(", ");
        for (RssList item : sources) {
            joiner.add(item.getSource());
        }
        nextButton(chatId, "<b>" + rssSourcesText + "</b>\n" + joiner);
    }

    private void startActions(Update update, long chatId) {
        addUser(update.getMessage());
        showLangButtons(chatId, "Choose your language");
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
    }

    private static String prepareTextToSave(String messageText) {
        return messageText.substring(messageText.indexOf(" ")).trim().toLowerCase();
    }

    private void initSearchButtons(long chatId) {
        showSearchAllButtons(chatId, getTextForAllSearch(chatId));
        showKeywordButtonsForSearch(chatId, getKeywordButtonsText(chatId));
    }

    private String getTextForAllSearch(long chatId) {
        return searchWithFilterText + " [<b>" + settingsRepository.getPeriodAllByChatId(chatId) + "</b>, " +
                searchWithFilter2Text + " - <b>" + excludedRepository.getExcludedCountByChatId(chatId) + "</b>]";
    }

    private String getKeywordButtonsText(long chatId) {
        return keywordSearchText + " [<b>" + settingsRepository.getPeriodByChatId(chatId) + "</b>, " +
                keywordSearch2Text + " - <b>" + keywordRepository.getKeywordsCountByChatId(chatId) + "</b>]";
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
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (!keywordsByChatId.isEmpty()) {
            String text = "<b>" + keywordsListText + "</b> [" + keywordsByChatId.size() + "]\n" +
                    keywordsByChatId
                            .toString()
                            .replace("[", "")
                            .replace("]", "");
            showKeywordButtons(chatId, text);
        } else {
            showAddKeywordsButton(chatId, keywordsNotSetText);
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
            showExcludedButtons(chatId, "<b>" + exclusionWordsText + "</b> [" + excludedCount +"]\n" +
                    joiner);
        } else {
            showExcludeButton(chatId);
        }
    }

    private static String parseMessageText(String messageText) {
        return messageText.substring(messageText.indexOf(" "))
                .replaceAll(" ", "")
                .replace("_", " ") // for add words with space: spb_exchange -> spb exch exchange
                .trim()
                .toLowerCase();
    }

    public static SendMessage prepareMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        return message;
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
        }

    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = prepareMessage(chatId, textToSend);
        message.enableHtml(true);
        message.disableWebPagePreview();
        executeMessage(message);
    }

    protected void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    private void addTopTen(long chatId, ArrayList<String> words) {
        int counter = 0;
        Set<String> topTenWordsByChatId = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        TopTenExcluded topTenExcluded;
        for (String word : words) {

            if (!(word.length() <= 2)) {
                if (!topTenWordsByChatId.contains(word)) {
                    topTenExcluded = new TopTenExcluded();
                    topTenExcluded.setUserId(chatId);
                    topTenExcluded.setWord(word);

                    try {
                        topTenRepository.save(topTenExcluded);
                        counter++;
                    } catch (Exception e) {
                        if (e.getMessage().contains("ui_top_ten_excluded")) {
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
            sendMessage(chatId, EmojiParser.parseToUnicode(wordsAddedText + counter + " ✔️"));
        } else {
            sendMessage(chatId, wordsIsNotAddedText);
        }
        prefix = "";
    }

    private void addKeyword(long chatId, String[] keywords) {
        int counter = 0;
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        Keyword word;
        for (String keyword : keywords) {
            keyword = keyword.toLowerCase();
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
            sendMessage(chatId, EmojiParser.parseToUnicode(wordsAddedText + counter + " ✔️"));
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
            word = word.toLowerCase();
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
            sendMessage(chatId, EmojiParser.parseToUnicode(addedExceptionWordsText + " - " + counter + " ✔️"));
        } else {
            sendMessage(chatId, wordsIsNotAddedText);
        }

        prefix = "";
    }

    private void delKeyword(long chatId, String[] keywords) {
        for (String keyword : keywords) {
            if (keyword.equals("*")) {
                keywordRepository.deleteAllKeywordsByChatId(chatId);
                sendMessage(chatId, deleteAllKeywordsText);
                break;
            }

            if (keywordRepository.isKeywordExists(chatId, keyword) > 0) {
                keywordRepository.deleteKeywordByChatId(chatId, keyword);
                sendMessage(chatId, EmojiParser.parseToUnicode(wordDeletedText + " - " + keyword + " ❌"));
            } else {
                sendMessage(chatId, String.format(wordIsNotInTheListText, keyword));
            }
        }
        prefix = "";
    }

    private void delExcluded(long chatId, String[] excluded) {
        for (String exclude : excluded) {
            if (exclude.equals("*")) {
                excludedRepository.deleteAllExcludedByChatId(chatId);
                sendMessage(chatId, deleteAllExcludedText);
                break;
            }

            if (excludedRepository.isWordExists(exclude) > 0) {
                excludedRepository.deleteExcludedByChatId(chatId, exclude);
                sendMessage(chatId, EmojiParser.parseToUnicode("Удалено слово - " + exclude + " ❌"));
            } else {
                sendMessage(chatId, "Слово " + exclude + " отсутствует в списке");
            }
        }
        prefix = "";
    }

    private void addSettings(long chatId) {
        Settings settings = new Settings();
        settings.setChatId(chatId);
        settings.setPeriod("6h");
        settings.setPeriodAll("1h");
        settings.setScheduler("on");
        settings.setStart(LocalTime.of(14, 0));
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
        if (!isAutoSearch.get()) {
            sendMessage(chatId, searchByKeywordsStartText);
        }

        if (keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
            showAddKeywordsButton(chatId, setupKeywordsText);
            return 0;
        }

        // Search
        Set<Headline> headlines = search.start(chatId, "keywords");

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
        sendMessage(1254981379, "<b>Message</b> from " + chatId + "\n" + text);
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

    public void showTodoButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TODO_DEL", delText);
        buttons.put("TODO_ADD", addText);
        buttons.put("TODO_LIST", listText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showTodoAddButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "<b>" + listTodoIsEmptyText + "</b>");
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TODO_ADD", addText);

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

    public void getNumberTopSearchByWord(long chatId) {
        SendMessage message = prepareMessage(chatId, chooseNumberWordFromTop);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        buttons1.put("TOP_NUM_1", "1");
        buttons1.put("TOP_NUM_2", "2");
        buttons1.put("TOP_NUM_3", "3");
        buttons1.put("TOP_NUM_4", "4");
        buttons1.put("TOP_NUM_5", "5");
        buttons1.put("TOP_NUM_6", "6");
        buttons1.put("TOP_NUM_7", "7");
        buttons1.put("TOP_NUM_8", "8");
        buttons2.put("TOP_NUM_9", "9");
        buttons2.put("TOP_NUM_10", "10");
        buttons2.put("TOP_NUM_11", "11");
        buttons2.put("TOP_NUM_12", "12");
        buttons2.put("TOP_NUM_13", "13");
        buttons2.put("TOP_NUM_14", "14");
        buttons3.put("TOP_NUM_15", "15");
        buttons3.put("TOP_NUM_16", "16");
        buttons3.put("TOP_NUM_17", "17");
        buttons3.put("TOP_NUM_18", "18");
        buttons3.put("TOP_NUM_19", "19");
        buttons3.put("TOP_NUM_20", "20");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3));
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

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_EXCLUDED", delText);
        buttons.put("EXCLUDE", addText);
        buttons.put("FIND_ALL", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
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

    private void showSearchAllButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("LIST_EXCLUDED", excludedText);
        buttons.put("FIND_ALL", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtonsForSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("LIST_KEYWORDS", listKeywordsText);
        buttons.put("FIND_BY_KEYWORDS", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE", delText);
        buttons.put("ADD", addText);
        buttons.put("FIND_BY_KEYWORDS", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
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

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons, buttons2, buttons3));
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
        SendMessage message = prepareMessage(chatId, aboutDeveloperText);
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
        buttons.put("LIST_KEYWORDS", listKeywordsText);
        buttons.put("FIND_BY_KEYWORDS", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", excludeWordText);
        buttons.put("FIND_ALL", searchText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getReplyKeywordWithSearch(long chatId, String textToSend, String firstName) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(String.format(textToSend, firstName));

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(fullSearchText);
        row.add(keywordsSearchText);
        row.add(topText);

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    // Показать топ слов за период
    private void showTopTenButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();

        buttons1.put("LIST_TOP", excludedListText);
        buttons1.put("ADD_TOP", delFromTopText);
        buttons2.put("WORD_SEARCH", searchByTopWordText);
        buttons2.put("GET_TOP", updateTopText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, null));
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
        buttons.put("ADD_TOP", addText);
        buttons.put("GET_TOP", topText);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getTopTenWordsList(long chatId) {
        Set<String> terms = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        showTopTenListButtons(chatId, "<b>" + listOfDeletedFromTopText + "</b> [" + terms.size() + "]\n" +
                terms.stream()
                        .limit(TOP_TEN_LIST_LIMIT)
                        .toList()
                        .toString()
                        .replace("[", "")
                        .replace("]", "")
        );
    }

    private void showTopTen(long chatId) {
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

            showTopTenButtons(chatId, "<b>" + top20ByPeriodText +
                    settingsRepository.getPeriodAllByChatId(chatId) + "</b>\n" + stringBuilder);
        }
    }

    private List<String> getTopTen(long chatId) {
        Map<String, Integer> wordsCount = new HashMap<>();

        for (Headline headline : Search.headlinesTopTen) {
            String[] titles = headline.getTitle().replaceAll("[,|:]", "").toLowerCase().split(" ");
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
            if (topTenRepository.isWordExists(chatId, word) > 0) {
                topTenRepository.deleteWordByChatId(chatId, word);
                sendMessage(chatId, EmojiParser.parseToUnicode(wordDeletedText + " - " + word + " ❌"));
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
                int counter = findNewsByKeywords(setting.getChatId());
                if (counter > 0) log.warn("Автопоиск ключевых слов. Пользователь: {}, найдено {}", username, counter);
            }
        }
        isAutoSearch.set(false);
    }

}