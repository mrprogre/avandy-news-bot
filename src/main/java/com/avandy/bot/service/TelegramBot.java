package com.avandy.bot.service;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.InlineKeyboards;
import com.avandy.bot.utils.Text;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private Search search;
    private UserRepository userRepository;
    private KeywordRepository keywordRepository;
    private SettingsRepository settingsRepository;
    private ExcludedRepository excludedRepository;
    private RssRepository rssRepository;
    private TodoRepository todoRepository;
    public static AtomicBoolean isAutoSearch = new AtomicBoolean(false);
    static String prefix = "";

    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config) {
        super(botToken);
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/info", "Информация"));
        listOfCommands.add(new BotCommand("/settings", "Настройки"));
        listOfCommands.add(new BotCommand("/keywords", Text.LIST_KEYWORDS_RUS));
        listOfCommands.add(new BotCommand("/find", "Выбор вида поиска"));
        listOfCommands.add(new BotCommand("/excluded", "Слова исключения"));
        listOfCommands.add(new BotCommand("/rss", "Источники RSS"));
        listOfCommands.add(new BotCommand("/todo", "Список задач"));
        listOfCommands.add(new BotCommand("/start", "Запуск бота"));
        listOfCommands.add(new BotCommand("/delete", "Удалить данные пользователя"));

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

            } else if (messageText.startsWith("/update-start")) {
                try {
                    int start = Integer.parseInt(prepareTextToSave(messageText).replaceAll("\\D+", ""));

                    if (start < 0 || start > 23) {
                        sendMessage(chatId, "Указано некорректное время. " +
                                "Должна быть цифра от 0 до 23 включительно");
                        prefix = "/update-start ";
                    } else {
                        settingsRepository.updateStart(LocalTime.of(start, 0), chatId);
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
            }

            else {
                /* Команды без параметров */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId);
                    case "/find" -> initSearchButtons(chatId);
                    case "/info" -> infoButtons(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/keywords" -> getKeywordsList(chatId);
                    case "/rss" -> getRssList(chatId);
                    case "/excluded" -> getExcludedList(chatId);
                    case "/settings" -> getSettings(chatId);
                    default -> sendMessage(chatId, "Данная команда не существует");
                }
            }
            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case Text.START_SEARCH, "DELETE_NO" -> initSearchButtons(chatId);

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
                    cancelButton(chatId, "Введите слова для удаления из исключений (разделять запятой)");
                }

                /* KEYWORDS */
                case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywords(chatId)).start();
                case Text.LIST_KEYWORDS -> new Thread(() -> getKeywordsList(chatId)).start();
                case Text.ADD -> {
                    prefix = "/add-keywords ";
                    cancelButton(chatId, "Введите ключевые слова (разделять запятой)");
                }
                case "DELETE" -> {
                    prefix = "/remove-keywords ";
                    cancelButton(chatId, "Введите слова для удаления (разделять запятой)");
                }

                /* SETTINGS */
                case "GET_SETTINGS" -> getSettings(chatId);
                case "SET_PERIOD" -> getNumbersForKeywordsPeriodButtons(chatId);
                case "SET_PERIOD_ALL" -> getNumbersForAllPeriodButtons(chatId);
                case "SET_SCHEDULER" -> showOnOffScheduler(chatId);
                case "SCHEDULER_ON" -> {
                    settingsRepository.updateScheduler("on", chatId);
                    sendMessage(chatId, Text.SCHEDULER_CHANGED);
                    getSettings(chatId);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatId);
                    sendMessage(chatId, Text.SCHEDULER_CHANGED);
                    getSettings(chatId);
                }
                case "SCHEDULER_START" -> {
                    prefix = "/update-start ";
                    cancelButton(chatId, "Введите время старта автопоиска (число 0-23)");
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

                case "YES_BUTTON" -> showAddKeywordsButton(chatId, Text.YES_BUTTON_TEXT);
                case "NO_BUTTON" -> sendMessage(chatId, Text.NO_BUTTON_TEXT);
                case "DELETE_YES" -> removeUser(chatId);
            }
        }
    }

    private void updatePeriod(int period, long chatId) {
        settingsRepository.updatePeriod(period + "h", chatId);
        sendMessage(chatId, Text.UPDATE_PERIOD_CHANGED);
        getSettings(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, Text.UPDATE_PERIOD_CHANGED);
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
        Iterable<RssList> sources = rssRepository.findAllActiveRss();

        StringJoiner joiner = new StringJoiner(", ");
        for (RssList item : sources) {
            joiner.add(item.getSource());
        }
        nextButton(chatId, "<b>Источники новостей</b>\n" + joiner);
    }

    private void startActions(Update update, long chatId) {
        saveUserToDatabase(update.getMessage());
        getReplyKeywordWithSearch(chatId, Text.getString(update.getMessage().getChat().getFirstName()));
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
                        schedSettings = "<b>4. Старт</b> автопоиска: <b>" + x.getStart() + "</b>\n" +
                                "Часы запуска: <b>" + Common.getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>\n";
                    }

                    String text = Text.getSettingsText(x, schedSettings);
                    getSettingsButtons(chatId, text);
                },
                () -> sendMessage(chatId, "Настройки не обнаружены")
        );
    }

    private void getKeywordsList(long chatId) {
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);
        if (!keywordsByChatId.isEmpty()) {
            String text = "<b>Список ключевых слов</b>\n" +
                    keywordRepository.findKeywordsByChatId(chatId)
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
            log.warn("New user saved: " + user);
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

    private void addKeyword(long chatId, String[] keywords) {
        int counter = 0;
        List<String> keywordsByChatId = keywordRepository.findKeywordsByChatId(chatId);

        Keyword word;
        for (String keyword : keywords) {
            if (!keywordsByChatId.contains(keyword)) {
                word = new Keyword();
                word.setChatId(chatId);
                word.setKeyword(keyword);
                keywordRepository.save(word);
                counter++;
            } else {
                sendMessage(chatId, "Слово <b>" + keyword + "</b> уже есть в списке");
            }
        }
        sendMessage(chatId, EmojiParser.parseToUnicode("Добавлено ключевых слов - " + counter + " ✔️"));
        prefix = "";
    }

    private void addExclude(long chatId, String[] list) {
        int counter = 0;
        List<String> excludedByChatId = excludedRepository.findExcludedByChatId(chatId);

        Excluded excluded;
        for (String word : list) {
            if (!excludedByChatId.contains(word)) {
                excluded = new Excluded();
                excluded.setChatId(chatId);
                excluded.setWord(word);
                excludedRepository.save(excluded);
                counter++;
            } else {
                sendMessage(chatId, "Слово <b>" + word + "</b> уже есть в списке");
            }
        }
        sendMessage(chatId, EmojiParser.parseToUnicode("Добавлено слов-исключений - " + counter + " ✔️"));
        prefix = "";
    }

    private void delKeyword(long chatId, String[] keywords) {
        for (String keyword : keywords) {
            if (keywordRepository.isKeywordExists(keyword) > 0) {
                keywordRepository.deleteKeywordByChatId(chatId, keyword);
                sendMessage(chatId, EmojiParser.parseToUnicode("Удалено слово - " + keyword + " ❌"));
            } else {
                sendMessage(chatId, "Cлово " + keyword + " отсутствует в списке");
            }
        }
        prefix = "";
    }

    private void delExcluded(long chatId, String[] excluded) {
        for (String exclude : excluded) {
            if (excludedRepository.isWordExists(exclude) > 0) {
                excludedRepository.deleteExcludedByChatId(chatId, exclude);
                sendMessage(chatId, EmojiParser.parseToUnicode("Удалено слово - " + exclude + " ❌"));
            } else {
                sendMessage(chatId, "Cлово " + exclude + " отсутствует в списке");
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
        settingsRepository.save(settings);
        prefix = "";
    }

    private void findAllNews(long chatId) {
        sendMessage(chatId, "» поиск всех новостей");
        Set<Headline> headlines = search.start(chatId, "show-all", "all");

        int counterForReduce = 1;
        int showAllCounter = 1;
        if (headlines.size() > 0) {
            // пока уберём и выведем кол-во новостей
            for (Headline headline : headlines) {

                sendMessage(chatId,
                        showAllCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                                Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                                headline.getTitle() + " " +
                                "<a href=\"" + headline.getLink() + "\">link</a>"
                );

                // Reducing the number of messages per second
                if (counterForReduce == 10) {
                    try {
                        Thread.sleep(1200);
                        counterForReduce = 1;
                    } catch (InterruptedException e) {
                        log.error(Common.ERROR_TEXT + e.getMessage());
                    }
                }
                counterForReduce++;
            }

            String text = "Найдено <b>" + Search.totalNewsCounter + "</b> " +
                    "(исключено <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>)";

            nextButtonAfterAllSearch(chatId, text);
        } else {
            String text = EmojiParser.parseToUnicode(Text.HEADLINES_NOT_FOUND);
            nextButtonAfterAllSearch(chatId, text);
        }

    }

    private void findNewsByKeywords(long chatId) {
        if (!isAutoSearch.get()) {
            sendMessage(chatId, "» поиск по ключевым словам");
        }

        if (keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
            showAddKeywordsButton(chatId, Text.SET_UP_KEYWORDS);
            return;
        }

        // Search
        Set<Headline> headlines = search.start(chatId, "show",
                "keywords");

        int showCounter = 1;
        if (headlines.size() > 0) {
            for (Headline headline : headlines) {
                sendMessage(chatId, showCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>"
                );
            }

            String text = "Найдено: <b>" + Search.filteredNewsCounter + "</b>";

            if (!isAutoSearch.get()) {
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        } else {
            if (!isAutoSearch.get()) {
                String text = EmojiParser.parseToUnicode(Text.HEADLINES_NOT_FOUND);
                nextButtonAfterKeywordsSearch(chatId, text);
            }
        }
        isAutoSearch.set(false);
    }

    private void sendFeedback(long chatId, String text) {
        sendMessage(1254981379, "<b>Message</b> from " + chatId + "\n" + text);
    }

    //@Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    @Scheduled(cron = "${cron.scheduler}")
    private void autoSearchByKeywords() {
        Integer hourNow = LocalTime.now().getHour();
        isAutoSearch.set(true);
        List<Settings> usersSettings = settingsRepository.findAllByScheduler();

        for (Settings setting : usersSettings) {
            log.warn("Scheduler: " + setting.getChatId());
            List<Integer> timeToExecute = Common.getTimeToExecute(setting.getStart(), setting.getPeriod());

            if (timeToExecute.contains(hourNow)) {
                findNewsByKeywords(setting.getChatId());
            }
        }
    }

    public void showOnOffScheduler(long chatId) {
        SendMessage message = prepareMessage(chatId, "Автопоиск");

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");

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
        buttons.put("TODO_DEL", "Удалить");
        buttons.put("TODO_ADD", Text.ADD_RUS);
        buttons.put("TODO_LIST", "Список");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    public void showTodoAddButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "<b>Список задач пуст</b>");
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("TODO_ADD", Text.ADD_RUS);

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
        buttons.put("DELETE_EXCLUDED", "Удалить");
        buttons.put("EXCLUDE", Text.ADD_RUS);
        buttons.put("FIND_ALL", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showExcludeButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "» слова-исключения не заданы");
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", Text.ADD_RUS);

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
        buttons.put(Text.LIST_KEYWORDS, Text.LIST_KEYWORDS_RUS);
        buttons.put("FIND_BY_KEYWORDS", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showKeywordButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE", "Удалить");
        buttons.put(Text.ADD, Text.ADD_RUS);
        buttons.put("FIND_BY_KEYWORDS", "Поиск");

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void showAddKeywordsButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(Text.ADD, Text.ADD_RUS);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void getSettingsButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);
        boolean isOn = settingsRepository.getSchedulerOnOffByChatId(chatId).equals("on");

        Map<String, String> buttons = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        buttons.put("SET_PERIOD", "1. Интервал (ключевые)");
        buttons.put("SET_PERIOD_ALL", "2. Интервал (общий)");
        buttons2.put("SET_SCHEDULER", "3. Автопоиск");

        if (isOn) {
            buttons2.put("SCHEDULER_START", "4. Старт");
            buttons2.put(Text.START_SEARCH, Text.NEXT_ICON);
        } else {
            buttons2.put(Text.START_SEARCH, Text.NEXT_ICON);
        }

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons, buttons2));
        executeMessage(message);
    }

    private void nextButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(Text.START_SEARCH, Text.NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void infoButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, Text.INFO);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", "Предложить идею");
        buttons.put(Text.START_SEARCH, Text.NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterKeywordsSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put(Text.LIST_KEYWORDS, Text.LIST_KEYWORDS_RUS);
        buttons.put(Text.START_SEARCH, Text.NEXT_ICON);

        message.setReplyMarkup(InlineKeyboards.inlineKeyboardMaker(buttons));
        executeMessage(message);
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", "Исключить слова");
        buttons.put(Text.START_SEARCH, Text.NEXT_ICON);

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

}