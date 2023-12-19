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
    public static final int TOP_TEN_LIMIT = 60;
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
    static String prefix = "";

    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config) {
        super(botToken);
        this.config = config;

        List<BotCommand> listOfCommands = new LinkedList<>();
        listOfCommands.add(new BotCommand("/settings", SETTINGS_RUS));
        listOfCommands.add(new BotCommand("/excluded", LIST_EXCLUDED_RUS));
        listOfCommands.add(new BotCommand("/keywords", LIST_KEYWORDS_RUS));
        listOfCommands.add(new BotCommand("/find", FIND_RUS));
        listOfCommands.add(new BotCommand("/top10", TOP_10_RUS));
        listOfCommands.add(new BotCommand("/rss", LIST_RSS_RUS));
        listOfCommands.add(new BotCommand("/todo", LIST_TODO_RUS));
        listOfCommands.add(new BotCommand("/info", INFO_RUS));
        listOfCommands.add(new BotCommand("/start", START_RUS));
        //listOfCommands.add(new BotCommand("/delete", DELETE_USER_RUS));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
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
            String firstName = update.getMessage().getChat().getFirstName();
            log.warn(firstName + ": [" + messageText + "]");

            if (messageText.startsWith("/send-feedback")) {
                String feedback = messageText.substring(messageText.indexOf(" ") + 1);
                sendFeedback(chatId, feedback);
                prefix = "";

            } else if (messageText.equals("/todo")) {
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

            } else if (messageText.startsWith("/addtop10") && messageText.length() > 9 && messageText.charAt(9) == ' ') {
                String exclude = parseMessageText(messageText);
                String[] words = exclude.split(",");
                addTopTen(chatId, words);
                showTopTen(chatId);

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
                        sendMessage(chatId, "Указано некорректное время. " +
                                "Должна быть цифра от 0 до 23 включительно");
                        prefix = "/update-start ";
                    } else {
                        settingsRepository.updateStart(LocalTime.of(start, 0, 0), chatId);
                        sendMessage(chatId, EmojiParser.parseToUnicode("Время старта автопоиска установлено ✔️"));
                        getSettings(chatId);
                        prefix = "";
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            } else if (messageText.startsWith("Поиск по словам")) {
                new Thread(() -> findNewsByKeywords(chatId)).start();

            } else if (messageText.startsWith("Поиск общий")) {
                new Thread(() -> findAllNews(chatId)).start();
            } else {
                /* Команды без параметров */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId);
                    case "/settings" -> new Thread(() -> getSettings(chatId)).start();
                    case "/info" -> new Thread(() -> infoButtons(chatId)).start();
                    case "/find" -> initSearchButtons(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/keywords" -> getKeywordsList(chatId);
                    case "/top10" -> showTopTen(chatId);
                    case "/rss" -> getRssList(chatId);
                    case "/excluded" -> getExcludedList(chatId);
                    default -> sendMessage(chatId, "Данная команда не существует");
                }
            }
            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case START_SEARCH, "DELETE_NO" -> initSearchButtons(chatId);

                /* FULL SEARCH */
                case "FIND_ALL" -> new Thread(() -> findAllNews(chatId)).start();

                /* EXCLUDED */
                case "LIST_EXCLUDED" -> new Thread(() -> getExcludedList(chatId)).start();
                case "EXCLUDE" -> {
                    prefix = "/add-excluded ";
                    cancelButton(chatId, "Введите слова для добавления в исключения (разделять запятой)");
                }
                case "DELETE_EXCLUDED" -> {
                    prefix = "/remove-excluded ";
                    cancelButton(chatId, "Введите слова для удаления из исключений (разделять запятой)\n" +
                            "* - удалить всё");
                }

                /* KEYWORDS */
                case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywords(chatId)).start();
                case LIST_KEYWORDS -> new Thread(() -> getKeywordsList(chatId)).start();
                case ADD -> {
                    prefix = "/add-keywords ";
                    cancelButton(chatId, "Введите ключевые слова (разделять запятой)");
                }
                case "DELETE" -> {
                    prefix = "/remove-keywords ";
                    cancelButton(chatId, "Введите слова для удаления (разделять запятой)\n" +
                            "* - удалить всё");
                }

                /* SETTINGS */
                case "GET_SETTINGS" -> getSettings(chatId);
                case "SET_PERIOD" -> getNumbersForKeywordsPeriodButtons(chatId);
                case "SET_PERIOD_ALL" -> getNumbersForAllPeriodButtons(chatId);
                case "SET_SCHEDULER" -> showOnOffScheduler(chatId);
                case "SCHEDULER_ON" -> {
                    settingsRepository.updateScheduler("on", chatId);
                    sendMessage(chatId, SCHEDULER_CHANGED);
                    getSettings(chatId);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatId);
                    sendMessage(chatId, SCHEDULER_CHANGED);
                    getSettings(chatId);
                }
                case "SCHEDULER_START" -> {
                    prefix = "/update-start ";
                    cancelButton(chatId, "Введите время старта автопоиска (число 0-23)");
                }
                case "SET_EXCLUDED" -> showOnOffExcluded(chatId);
                case "EXCLUDED_ON" -> {
                    settingsRepository.updateExcluded("on", chatId);
                    sendMessage(chatId, EXCLUDED_CHANGED);
                    getSettings(chatId);
                }
                case "EXCLUDED_OFF" -> {
                    settingsRepository.updateExcluded("off", chatId);
                    sendMessage(chatId, EXCLUDED_CHANGED);
                    getSettings(chatId);
                }

                case "TODO_LIST" -> getTodoList(chatId);
                case "TODO_ADD" -> {
                    prefix = "/addtodo ";
                    cancelButton(chatId, "Введите задачу (если несколько - через точку запятую)");
                }
                case "TODO_DEL" -> {
                    prefix = "/deltodo ";
                    cancelButton(chatId, "Введите номер задачи (если несколько - через запятую)");
                }

                case "FEEDBACK" -> {
                    prefix = "/send-feedback ";
                    cancelButton(chatId, "Напишите свои предложения разработчику");
                }

                case "CANCEL" -> {
                    prefix = "";
                    nextButton(chatId, "Действие отменено");
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

                case "YES_BUTTON" -> showAddKeywordsButton(chatId, YES_BUTTON_TEXT);
                case "NO_BUTTON" -> sendMessage(chatId, NO_BUTTON_TEXT);
                case "DELETE_YES" -> removeUser(chatId);

                case "ADD_TOP10" -> {
                    prefix = "/addtop10 ";
                    cancelButton(chatId, "Введите слово (если несколько - через запятую)");
                }
                case "LIST_TOP10" -> getTopTenWordsList(chatId);
                case "GET_TOP10" -> showTopTen(chatId);
                case "DELETE_TOP10" -> {
                    prefix = "/remove-top-ten ";
                    cancelButton(chatId, "Введите слова для удаления (разделять запятой)");
                }
            }
        }
    }

    private void updatePeriod(int period, long chatId) {
        settingsRepository.updatePeriod(period + "h", chatId);
        sendMessage(chatId, UPDATE_PERIOD_CHANGED);
        getSettings(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, UPDATE_PERIOD_CHANGED);
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
        sendMessage(chatId, "Задачи удалены");
    }

    private void addTodo(String messageText, long chatId) {
        String[] texts = messageText
                .substring(messageText.indexOf(" "))
                .split(";");

        for (String text : texts) {
            todoRepository.save(new Todo(chatId, text.trim()));
        }
        sendMessage(chatId, "Список задач дополнен");
    }

    private void getTodoList(long chatId) {
        List<Todo> todoList = todoRepository.findByChatId(chatId);

        if (todoList.size() > 0) {
            StringJoiner joiner = new StringJoiner("\n");
            for (Todo item : todoList) {
                joiner.add(item.getId() + ". " + item.getText() + " (добавлена " + item.getAddDate() + ")");
            }
            showTodoButtons(chatId, "<b>Список задач</b>\n" + joiner);

        } else {
            showTodoAddButton(chatId);
        }
    }

    private void removeUser(long chatId) {
        log.warn("User {} deleted account", userRepository.findById(chatId));
        userRepository.deleteById(chatId);
        String text = EmojiParser.parseToUnicode("Пока! \uD83D\uDC4B");
        sendMessage(chatId, text);
    }

    private void getRssList(long chatId) {
        Iterable<RssList> sources = rssRepository.findAllActiveSources();

        StringJoiner joiner = new StringJoiner(", ");
        for (RssList item : sources) {
            joiner.add(item.getSource());
        }
        nextButton(chatId, "<b>Источники новостей</b>\n" + joiner);
    }

    private void startActions(Update update, long chatId) {
        saveUserToDatabase(update.getMessage());
        getReplyKeywordWithSearch(chatId, getString(update.getMessage().getChat().getFirstName()));
        showYesNoOnStart(chatId, "Продолжим?");
    }

    private static String prepareTextToSave(String messageText) {
        return messageText.substring(messageText.indexOf(" ")).trim().toLowerCase();
    }

    private void initSearchButtons(long chatId) {
        showSearchAllButtons(chatId, getTextForAllSearch(chatId));
        showKeywordButtonsForSearch(chatId, getKeywordButtonsText(chatId));
    }

    private String getTextForAllSearch(long chatId) {
        return "Поиск: <b>общий с фильтрацией</b>\n[период: текущее время минус <b>" + settingsRepository.getPeriodAllByChatId(chatId) +
                "</b>, слов-исключений - <b>" + excludedRepository.getExcludedCountByChatId(chatId) + "</b>]";
    }

    private String getKeywordButtonsText(long chatId) {
        return "Поиск: <b>по ключевым словам</b>\n[период: текущее время минус <b>" +
                settingsRepository.getPeriodByChatId(chatId) +
                "</b>, ключевых слов - <b>" + keywordRepository.getKeywordsCountByChatId(chatId) + "</b>]";
    }

    private void getSettings(long chatId) {
        Optional<Settings> sets = settingsRepository.findById(chatId).stream().findFirst();

        sets.ifPresentOrElse(x -> {
                    String schedSettings = "";

                    if (x.getScheduler().equals("on")) {
                        String text;
                        if (x.getPeriod().equals("1h")) {
                            text = " (запуск каждый час)\n";
                        } else if (x.getPeriod().equals("2h")) {
                            text = " (запуск каждые 2 часа)\n";
                        } else {
                            text = " (часы запуска: <b>" + Common.getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>)\n";
                        }

                        schedSettings = "- - - - - -\n" + "<b>5. Старт</b> автопоиска: <b>" + x.getStart() + "</b>" + text;
                    }

                    String text = getSettingsText(x, schedSettings);
                    getSettingsButtons(chatId, text);
                },
                () -> sendMessage(chatId, "Настройки не обнаружены")
        );
    }

    private void getKeywordsList(long chatId) {
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        if (!keywordsByChatId.isEmpty()) {
            String text = "<b>Список ключевых слов</b>\n" +
                    keywordsByChatId
                            .toString()
                            .replace("[", "")
                            .replace("]", "");
            showKeywordButtons(chatId, text);
        } else {
            showAddKeywordsButton(chatId, "» ключевые слова не заданы");
        }
    }

    private void getExcludedList(long chatId) {
        List<String> excludedByChatId = excludedRepository.findExcludedByChatId(chatId);
        if (excludedByChatId.size() >= 400) {
            sendMessage(chatId, "Количество слов-исключений <b>" + excludedByChatId.size() +
                    "</b>\nПокажу последние <b>30</b>");
            excludedByChatId = excludedRepository.findExcludedByChatIdLimit(chatId);
        }

        if (!excludedByChatId.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (String item : excludedByChatId) {
                joiner.add(item);
            }
            showExcludedButtons(chatId, "<b>Слова-исключения</b>\n" + joiner);
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

    private void saveUserToDatabase(Message message) {
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

    private void addTopTen(long chatId, String[] words) {
        int counter = 0;
        Set<String> topTenWordsByChatId = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);

        TopTenExcluded topTenExcluded;
        for (String word : words) {
            word = word.toLowerCase();

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
                            log.info(WORD_IS_EXISTS + word);
                        }
                    }

                } else {
                    sendMessage(chatId, WORD_IS_EXISTS + word);
                }
            } else {
                sendMessage(chatId, MIN_WORD_LENGTH);
            }

        }
        if (counter != 0) {
            sendMessage(chatId, EmojiParser.parseToUnicode(WORDS_ADDED + counter + " ✔️"));
        } else {
            sendMessage(chatId, WORDS_IS_NOT_ADD);
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
                            log.info(WORD_IS_EXISTS + keyword);
                        }
                    }

                } else {
                    sendMessage(chatId, WORD_IS_EXISTS + keyword);
                }
            } else {
                sendMessage(chatId, MIN_WORD_LENGTH);
            }
        }
        if (counter != 0) {
            sendMessage(chatId, EmojiParser.parseToUnicode(WORDS_ADDED + counter + " ✔️"));
        } else {
            sendMessage(chatId, WORDS_IS_NOT_ADD);
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
                            log.info(WORD_IS_EXISTS + word);
                        }
                    }

                } else {
                    sendMessage(chatId, WORD_IS_EXISTS + word);
                }
            } else {
                sendMessage(chatId, MIN_WORD_LENGTH);
            }
        }

        if (counter != 0) {
            sendMessage(chatId, EmojiParser.parseToUnicode("Добавлено слов-исключений - " + counter + " ✔️"));
        } else {
            sendMessage(chatId, WORDS_IS_NOT_ADD);
        }

        prefix = "";
    }

    private void delKeyword(long chatId, String[] keywords) {
        for (String keyword : keywords) {
            if (keyword.equals("*")) {
                keywordRepository.deleteAllKeywordsByChatId(chatId);
                sendMessage(chatId, DELETE_ALL_KEYWORDS);
                break;
            }

            if (keywordRepository.isKeywordExists(chatId, keyword) > 0) {
                keywordRepository.deleteKeywordByChatId(chatId, keyword);
                sendMessage(chatId, EmojiParser.parseToUnicode("Удалено слово - " + keyword + " ❌"));
            } else {
                sendMessage(chatId, "Слово " + keyword + " отсутствует в списке");
            }
        }
        prefix = "";
    }

    private void delExcluded(long chatId, String[] excluded) {
        for (String exclude : excluded) {
            if (exclude.equals("*")) {
                excludedRepository.deleteAllExcludedByChatId(chatId);
                sendMessage(chatId, DELETE_ALL_EXCLUDED);
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
        settings.setPeriod("72h");
        settings.setPeriodAll("1h");
        settings.setScheduler("on");
        settings.setStart(LocalTime.of(14, 0));
        settings.setExcluded("on");
        settingsRepository.save(settings);
        prefix = "";
    }

    private void findAllNews(long chatId) {
        sendMessage(chatId, "» поиск всех новостей");
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

            String text = "Найдено <b>" + Search.totalNewsCounter + "</b> " +
                    "(исключено <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>)";

            nextButtonAfterAllSearch(chatId, text);
        } else {
            String text = EmojiParser.parseToUnicode(HEADLINES_NOT_FOUND);
            nextButtonAfterAllSearch(chatId, text);
        }

    }

    private int findNewsByKeywords(long chatId) {
        if (!isAutoSearch.get()) {
            sendMessage(chatId, "» поиск по ключевым словам");
        }

        if (keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
            showAddKeywordsButton(chatId, SET_UP_KEYWORDS);
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

            String text = "Найдено: <b>" + Search.filteredNewsCounter + "</b>";

            if (!isAutoSearch.get()) {
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        } else {
            if (!isAutoSearch.get()) {
                String text = EmojiParser.parseToUnicode(HEADLINES_NOT_FOUND);
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        }
        return Search.filteredNewsCounter;
    }

    private void showTopTen(long chatId) {
        // init
        search.start(chatId, "top10");

        List<String> topTen = getTopTen(chatId);
        if (topTen.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : topTen) {
                stringBuilder.append(s);
            }
            showTopTenButtons(chatId, "<b>Топ 10 слов за " + settingsRepository.getPeriodAllByChatId(chatId) +
                    "</b>\n" + stringBuilder);
        }
    }

    private void sendFeedback(long chatId, String text) {
        sendMessage(1254981379, "<b>Message</b> from " + chatId + "\n" + text);
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

    public void showOnOffScheduler(long chatId) {
        SendMessage message = prepareMessage(chatId, "Автопоиск");

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showOnOffExcluded(long chatId) {
        SendMessage message = prepareMessage(chatId, "Исключение заголовков");

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDED_OFF", "Off");
        buttons.put("EXCLUDED_ON", "On");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showYesNoOnStart(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("NO_BUTTON", "Нет");
        buttons.put("YES_BUTTON", "Да");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showTodoButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TODO_DEL", DEL_RUS);
        buttons.put("TODO_ADD", ADD_RUS);
        buttons.put("TODO_LIST", "Список");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showTodoAddButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "<b>Список задач пуст</b>");
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TODO_ADD", ADD_RUS);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showYesNoOnDeleteUser(long chatId) {
        SendMessage message = prepareMessage(chatId, "Подтверждаете удаление пользователя?");

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_YES", "Да");
        buttons.put("DELETE_NO", "Нет");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void getNumbersForKeywordsPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, "Выберите глубину поиска в часах");

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
        SendMessage message = prepareMessage(chatId, "Выберите глубину поиска в часах");

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

    private void cancelButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("CANCEL", "Отменить");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showExcludedButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_EXCLUDED", DEL_RUS);
        buttons.put("EXCLUDE", ADD_RUS);
        buttons.put("FIND_ALL", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showExcludeButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "» слова-исключения не заданы");
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", ADD_RUS);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showSearchAllButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("LIST_EXCLUDED", "Исключения");
        buttons.put("FIND_ALL", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtonsForSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(LIST_KEYWORDS, LIST_KEYWORDS_RUS);
        buttons.put("FIND_BY_KEYWORDS", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE", DEL_RUS);
        buttons.put(ADD, ADD_RUS);
        buttons.put("FIND_BY_KEYWORDS", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showAddKeywordsButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(ADD, ADD_RUS);

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
        buttons.put("SET_PERIOD", "1. Интервал");
        buttons.put("SET_PERIOD_ALL", "2. Интервал");
        buttons2.put("SET_SCHEDULER", "3. Автопоиск");
        buttons2.put("SET_EXCLUDED", "4. Исключение");

        if (isOn) {
            buttons3.put("SCHEDULER_START", "5. Старт");
            buttons3.put(START_SEARCH, NEXT_ICON);
        } else {
            buttons3.put(START_SEARCH, NEXT_ICON);
        }

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons, buttons2, buttons3));
        executeMessage(message);
    }

    private void nextButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(START_SEARCH, NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void infoButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, INFO);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", "Предложить идею");
        buttons.put(START_SEARCH, NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterKeywordsSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(LIST_KEYWORDS, LIST_KEYWORDS_RUS);
        buttons.put("FIND_BY_KEYWORDS", FIND_AGAIN_RUS);
        //buttons.put(START_SEARCH, NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", "Исключить слова");
        buttons.put("FIND_ALL", FIND_AGAIN_RUS);
        //buttons.put(START_SEARCH, NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getReplyKeywordWithSearch(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Поиск общий");
        row.add("Поиск по словам");

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void showTopTenButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD_TOP10", DEL_RUS);
        buttons.put("LIST_TOP10", "Список");
        buttons.put("GET_TOP10", "Топ 10");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showTopTenListButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_TOP10", DEL_RUS);
        buttons.put("ADD_TOP10", ADD_RUS);
        buttons.put("GET_TOP10", "Топ 10");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getTopTenWordsList(long chatId) {
        showTopTenListButtons(chatId, "<b>Список удалённого из Топ 10</b>\n" +
                topTenRepository.findAllExcludedFromTopTenByChatId(chatId).stream()
                        .limit(TOP_TEN_LIMIT)
                        .toList()
                        .toString()
                        .replace("[", "")
                        .replace("]", "")
        );
    }

    private List<String> getTopTen(long chatId) {
        Map<String, Integer> wordsCount = new HashMap<>();

        for (Headline headline : Search.headlinesForTopTen) {
            String[] substr = headline.getTitle().split(" ");
            for (String s : substr) {
                if (s.length() > 2) {
                    wordsCount.put(s.toLowerCase(), wordsCount.getOrDefault(s, 0) + 1);
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
                .limit(10)
                .map(x -> {
                    String format;
                    int length = x.getValue().toString().length();
                    if (length == 1) {
                        format = "%-7d %s";
                    } else if (length == 2) {
                        format = "%-6d %s";
                    } else {
                        format = "%-5d %s";
                    }
                    return String.format(format, x.getValue(), x.getKey()) + "\n";
                })
                .toList();
    }

    private void delFromTopTenList(long chatId, String[] words) {
        for (String word : words) {
            if (topTenRepository.isWordExists(chatId, word) > 0) {
                topTenRepository.deleteWordByChatId(chatId, word);
                sendMessage(chatId, EmojiParser.parseToUnicode("Удалено слово - " + word + " ❌"));
            } else {
                sendMessage(chatId, "Слово " + word + " отсутствует в списке");
            }
        }
        prefix = "";
    }
}