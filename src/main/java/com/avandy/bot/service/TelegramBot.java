package com.avandy.bot.service;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    static boolean test = false;
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
    private final String headlinesNotFound = EmojiParser.parseToUnicode("» новости не найдены \uD83D\uDCA4");
    String setUpKeywords = "» ключевые слова не заданы";
    String updatePeriodChanged = EmojiParser.parseToUnicode("Интервал поиска изменён ✔️");

    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config) {
        super(botToken);
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/info", "Информация"));
        listOfCommands.add(new BotCommand("/settings", "Настройки"));
        listOfCommands.add(new BotCommand("/keywords", "Ключевые слова"));
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

            /* Команды параметрами */
            // send message to all users
            if (messageText.contains("/send") && messageText.charAt(5) == ' ' && messageText.length() > 6
                    && config.getBotOwner() == chatId) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                Iterable<User> users = userRepository.findAllActiveUsers();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }

            } else if (messageText.startsWith("/send-feedback")) {
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

            } else {
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
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "START_SEARCH", "DELETE_NO" -> initSearchButtons(chatId);

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
                case "LIST_KEYWORDS" -> new Thread(() -> getKeywordsList(chatId)).start();
                case "ADD" -> {
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
                    sendMessage(chatId, EmojiParser.parseToUnicode("Режим автопоиска переключён ✔️"));
                    getSettings(chatId);
                }
                case "SCHEDULER_OFF" -> {
                    settingsRepository.updateScheduler("off", chatId);
                    sendMessage(chatId, EmojiParser.parseToUnicode("Режим автопоиска переключён ✔️"));
                    getSettings(chatId);
                }
                case "SCHEDULER_START" -> {
                    prefix = "/update-start ";
                    //getNumbersButtons(chatId);
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

                case "YES_BUTTON" -> {
                    String text = """
                            Первое, что нужно сделать, - это добавить <b>ключевые слова</b>.
                            Если интересуют новости, связанные с ипотекой, к примеру, то надо добавить слово <b>ипотек</b>, чтобы получить все заголовки, содержащие: <b>ипотекА, ипотекУ</b> и т.д.
                            - - - - - -
                            Второе - добавить через меню <b>слова-исключения</b> для полного поиска, чтобы не читать новости типа: "Как правильно красить ресницы". Просто добавь <b>ресниц</b> в исключения и таких заголовков больше не увидишь!
                            - - - - - -
                            Добавлять можно как одно слово, так и несколько через запятую.
                            - - - - - -
                            На данном этапе мы добавим <b>ключевые слова</b>. Нажми кнопку "Добавить".""";
                    showAddKeywordsButton(chatId, text);
                }
                case "NO_BUTTON" -> {
                    String text = EmojiParser.parseToUnicode("Good buy! \uD83D\uDC4B");
                    userRepository.deleteById(chatId);
                    executeEditMessageText((int) messageId, chatId, text);
                }
                case "DELETE_YES" -> removeUser(chatId);
            }
        }
    }

    private void updatePeriod(int period, long chatId) {
        settingsRepository.updatePeriod(period + "h", chatId);
        sendMessage(chatId, updatePeriodChanged);
        getSettings(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, updatePeriodChanged);
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

        String userFirstname = update.getMessage().getChat().getFirstName();

        String hello = EmojiParser.parseToUnicode("Здравствуй, " + userFirstname + "! :blush: \n" +
                "Я могу найти для тебя нужную информацию и отсеять много ненужной! Продолжим?");
        showYesNoOnStart(chatId, hello);
    }

    private static String prepareTextToSave(String messageText) {
        return messageText.substring(messageText.indexOf(" ")).trim().toLowerCase();
    }

    private void cancelButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отменить");
        cancelButton.setCallbackData("CANCEL");


        rowInLine.add(cancelButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
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

                    String text = "<b>Настройки</b>\n" +
                            "- - - - - -\n" +
                            "<b>1. Интервал</b> поиска (<b>ключевые слова</b>): минус <b>" + x.getPeriod() + "</b>\n" +
                            "<b>2. Интервал</b> поиска (<b>все новости</b>): минус <b>" + x.getPeriodAll() + "</b>\n" +
                            "<b>3. Автопоиск</b> по ключевым словам: <b>" + x.getScheduler() + "</b>\n" +
                            schedSettings +
                            "- - - - - -\n" +
                            "Параметры меняются после нажатия на кнопки";

                    getSettingsButtons(chatId, text);
                },
                () -> sendMessage(chatId, "Настройки не обнаружены")
        );
    }

    private void getSettingsButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);
        boolean isOn = settingsRepository.getSchedulerOnOffByChatId(chatId).equals("on");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton periodButton = new InlineKeyboardButton();
        periodButton.setText("1. Инт");
        periodButton.setCallbackData("SET_PERIOD");

        InlineKeyboardButton periodAllButton = new InlineKeyboardButton();
        periodAllButton.setText("2. Инт");
        periodAllButton.setCallbackData("SET_PERIOD_ALL");

        InlineKeyboardButton autoButton = new InlineKeyboardButton();
        autoButton.setText("3. Авто");
        autoButton.setCallbackData("SET_SCHEDULER");

        InlineKeyboardButton findButton = new InlineKeyboardButton();
        findButton.setText("» » »");
        findButton.setCallbackData("START_SEARCH");

        rowInLine.add(0, periodButton);
        rowInLine.add(1, periodAllButton);
        rowInLine.add(2, autoButton);

        InlineKeyboardButton startButton;
        if (isOn) {
            startButton = new InlineKeyboardButton();
            startButton.setText("4. Старт");
            startButton.setCallbackData("SCHEDULER_START");
            rowInLine.add(3, startButton);
            rowInLine.add(4, findButton);
        } else {
            rowInLine.add(3, findButton);
        }

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void nextButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText("» » »");
        nextButton.setCallbackData("START_SEARCH");

        rowInLine.add(nextButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void infoButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, """
                <b>Разработчик</b>: <a href="https://github.com/mrprogre">mrprogre</a>
                <b>Почта</b>: rps_project@mail.ru
                <b>Основная программа:</b> <a href="https://avandy-news.ru">Avandy News Analysis</a> (запись в Реестре российского ПО: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""");
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton sendDevButton = new InlineKeyboardButton();
        sendDevButton.setText("Предложить идею");
        sendDevButton.setCallbackData("FEEDBACK");

        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText("» » »");
        nextButton.setCallbackData("START_SEARCH");

        rowInLine.add(sendDevButton);
        rowInLine.add(nextButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void nextButtonAfterKeywordsSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("Ключевые слова");
        listButton.setCallbackData("LIST_KEYWORDS");

        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText("» » »");
        nextButton.setCallbackData("START_SEARCH");

        rowInLine.add(listButton);
        rowInLine.add(nextButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("Исключить слова");
        listButton.setCallbackData("EXCLUDE");

        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText("» » »");
        nextButton.setCallbackData("START_SEARCH");

        rowInLine.add(listButton);
        rowInLine.add(nextButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
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

    private void showExcludedButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить");
        deleteButton.setCallbackData("DELETE_EXCLUDED");

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("EXCLUDE");

        InlineKeyboardButton nextButton = new InlineKeyboardButton();
        nextButton.setText("Поиск");
        nextButton.setCallbackData("FIND_ALL");

        rowInLine.add(deleteButton);
        rowInLine.add(addButton);
        rowInLine.add(nextButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showExcludeButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "» слова-исключения не заданы");
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("EXCLUDE");

        rowInLine.add(addButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showSearchAllButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton excludeButton = new InlineKeyboardButton();
        excludeButton.setText("Исключения");
        excludeButton.setCallbackData("LIST_EXCLUDED");

        InlineKeyboardButton findButton = new InlineKeyboardButton();
        findButton.setText("Поиск");
        findButton.setCallbackData("FIND_ALL");

        rowInLine.add(excludeButton);
        rowInLine.add(findButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showKeywordButtonsForSearch(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("Ключевые слова");
        listButton.setCallbackData("LIST_KEYWORDS");

        InlineKeyboardButton showButton = new InlineKeyboardButton();
        showButton.setText("Поиск");
        showButton.setCallbackData("FIND_BY_KEYWORDS");

        rowInLine.add(listButton);
        rowInLine.add(showButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showKeywordButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить");
        deleteButton.setCallbackData("DELETE");

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("ADD");

        InlineKeyboardButton showButton = new InlineKeyboardButton();
        showButton.setText("Поиск");
        showButton.setCallbackData("FIND_BY_KEYWORDS");

        rowInLine.add(deleteButton);
        rowInLine.add(addButton);
        rowInLine.add(showButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showAddKeywordsButton(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("ADD");

        rowInLine.add(addButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private static String parseMessageText(String messageText) {
        return messageText.substring(messageText.indexOf(" "))
                .replaceAll(" ", "")
                .replace("_", " ") // for add words with space: spb_exchange -> spb exch exchange
                .trim()
                .toLowerCase();
    }

    private void executeEditMessageText(int messageId, long chatId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
    }

    private static SendMessage prepareMessage(long chatId, String textToSend) {
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

    private void showYesNoOnStart(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData("YES_BUTTON");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData("NO_BUTTON");

        rowInLine.add(noButton);
        rowInLine.add(yesButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showTodoButtons(long chatId, String text) {
        SendMessage message = prepareMessage(chatId, text);
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton delButton = new InlineKeyboardButton();
        delButton.setText("Удалить");
        delButton.setCallbackData("TODO_DEL");

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("TODO_ADD");

        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("Список");
        listButton.setCallbackData("TODO_LIST");

        rowInLine.add(delButton);
        rowInLine.add(addButton);
        rowInLine.add(listButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showTodoAddButton(long chatId) {
        SendMessage message = prepareMessage(chatId, "<b>Список задач пуст</b>");
        message.enableHtml(true);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData("TODO_ADD");

        rowInLine.add(addButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showYesNoOnDeleteUser(long chatId) {
        SendMessage message = prepareMessage(chatId, "Подтверждаете удаление пользователя?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData("DELETE_YES");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData("DELETE_NO");

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void getNumbersForKeywordsPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, "Выберите глубину поиска в часах");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("1");
        button1.setCallbackData("BUTTON_1");

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("2");
        button2.setCallbackData("BUTTON_2");

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("4");
        button4.setCallbackData("BUTTON_4");

        InlineKeyboardButton button12 = new InlineKeyboardButton();
        button12.setText("12");
        button12.setCallbackData("BUTTON_12");

        InlineKeyboardButton button24 = new InlineKeyboardButton();
        button24.setText("24");
        button24.setCallbackData("BUTTON_24");

        InlineKeyboardButton button48 = new InlineKeyboardButton();
        button48.setText("48");
        button48.setCallbackData("BUTTON_48");

        InlineKeyboardButton button72 = new InlineKeyboardButton();
        button72.setText("72");
        button72.setCallbackData("BUTTON_72");

        rowInLine.add(button1);
        rowInLine.add(button2);
        rowInLine.add(button4);
        rowInLine.add(button12);
        rowInLine.add(button24);
        rowInLine.add(button48);
        rowInLine.add(button72);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void getNumbersForAllPeriodButtons(long chatId) {
        SendMessage message = prepareMessage(chatId, "Выберите глубину поиска в часах");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("1");
        button1.setCallbackData("BUTTON_1_ALL");

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("2");
        button2.setCallbackData("BUTTON_2_ALL");

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("4");
        button4.setCallbackData("BUTTON_4_ALL");

        InlineKeyboardButton button6 = new InlineKeyboardButton();
        button6.setText("6");
        button6.setCallbackData("BUTTON_6_ALL");

        InlineKeyboardButton button8 = new InlineKeyboardButton();
        button8.setText("8");
        button8.setCallbackData("BUTTON_8_ALL");

        InlineKeyboardButton button12 = new InlineKeyboardButton();
        button12.setText("12");
        button12.setCallbackData("BUTTON_12_ALL");

        InlineKeyboardButton button24 = new InlineKeyboardButton();
        button24.setText("24");
        button24.setCallbackData("BUTTON_24_ALL");

        rowInLine.add(button1);
        rowInLine.add(button2);
        rowInLine.add(button4);
        rowInLine.add(button6);
        rowInLine.add(button8);
        rowInLine.add(button12);
        rowInLine.add(button24);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void showOnOffScheduler(long chatId) {
        SendMessage message = prepareMessage(chatId, "Автопоиск");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("On");
        yesButton.setCallbackData("SCHEDULER_ON");

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("Off");
        noButton.setCallbackData("SCHEDULER_OFF");

        rowInLine.add(noButton);
        rowInLine.add(yesButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
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
            String text = EmojiParser.parseToUnicode(headlinesNotFound);
            nextButtonAfterAllSearch(chatId, text);
        }

    }

    private void findNewsByKeywords(long chatId) {
        if (!isAutoSearch.get()) {
            sendMessage(chatId, "» поиск по ключевым словам");
        }

        if (keywordRepository.findKeywordsByChatId(chatId).isEmpty()) {
            showAddKeywordsButton(chatId, setUpKeywords);
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
                String text = EmojiParser.parseToUnicode(headlinesNotFound);
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
}