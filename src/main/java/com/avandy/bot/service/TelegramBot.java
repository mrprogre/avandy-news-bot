package com.avandy.bot.service;

import com.avandy.bot.config.BotConfig;
import com.avandy.bot.model.*;
import com.avandy.bot.repository.*;
import com.avandy.bot.utils.Common;
import com.avandy.bot.utils.InlineKeyboards;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.avandy.bot.utils.Text.*;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    public static final String REPLACE_ALL_TOP = "[\"}|]|\\[|]|,|\\.|:|«|!|\\?|»|\"|;]";
    private static final int JARO_WINKLER_LEVEL = 85;
    private static final int TOP_TEN_SHOW_LIMIT = 20;
    private static final int TOP_TEN_LIST_LIMIT = 60;
    private static final int EXCLUDING_TERMS_LIST_LIMIT = 60;
    private static final int EXCLUDED_LIMIT = 100;
    private static final int LIMIT_FOR_BREAKING_INTO_PARTS = 200;
    private static final int SLEEP_BETWEEN_SENDING = 25;
    private final BotConfig config;
    private Search search;
    private UserRepository userRepository;
    private KeywordRepository keywordRepository;
    private SettingsRepository settingsRepository;
    private ExcludingTermsRepository excludingTermsRepository;
    private RssRepository rssRepository;
    private TopTenRepository topTenRepository;
    private final AtomicBoolean isAutoSearch = new AtomicBoolean(false);
    private StringBuilder stringBuilderTop;
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private static final String ICON_SEARCH = "\uD83C\uDFB2";
    private static final String ICON_NEWS_FOUNDED = "\uD83C\uDF3F";
    public static final String ICON_END_SEARCH_NOT_FOUND = "\uD83D\uDCA4";
    public static final String ICON_GOOD_BYE = "\uD83D\uDC4B";
    public static final String ICON_SETTINGS = "\uD83C\uDF0D";
    public static final String ICON_TOP_20_FIRE = "\uD83D\uDD25";

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
    public void setExcludedRepository(ExcludingTermsRepository excludingTermsRepository) {
        this.excludingTermsRepository = excludingTermsRepository;
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
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
            UserState userState = userStates.get(chatId);

            if (config.getBotOwner() != chatId) {
                String firstName = update.getMessage().getChat().getFirstName();
                log.warn(firstName + ": [" + messageText + "]");
            }

            /* SEND TO ALL FROM BOT OWNER */
            if (messageText.startsWith(":") && config.getBotOwner() == chatId) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                List<User> users = userRepository.findAllByIsActive();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
            } else if (messageText.startsWith("@") && config.getBotOwner() == chatId) {
                long chatToSend = Long.parseLong(messageText.substring(1, messageText.indexOf(" ")));
                String textToSend = messageText.substring(messageText.indexOf(" "));
                sendMessage(chatToSend, textToSend);

                /* USER INPUT BLOCK */
            } else if (userState != null && "SEND_FEEDBACK".equals(userState.getState())) {
                String feedback = messageText.substring(messageText.indexOf(" ") + 1);
                sendFeedback(chatId, feedback);

            } else if (userState != null && "ADD_KEYWORDS".equals(userState.getState())) {
                String keywords = messageText.trim().toLowerCase();
                String[] words = keywords.split(",");
                addKeyword(chatId, words);
                showKeywordsList(chatId);

            } else if (userState != null && "DEL_KEYWORDS".equals(userState.getState())) {
                String keywords = messageText.trim().toLowerCase();
                removeKeywords(keywords, chatId);

            } else if (userState != null && "ADD_EXCLUDED".equals(userState.getState())) {
                String exclude = messageText.trim().toLowerCase();
                String[] words = exclude.split(",");
                addExclude(chatId, words);
                getExcludedList(chatId);

            } else if (userState != null && "DEL_EXCLUDED".equals(userState.getState())) {
                String excluded = messageText.trim().toLowerCase();
                String[] words = excluded.split(",");
                delExcluded(chatId, words);
                getExcludedList(chatId);

            } else if (userState != null && "DEL_TOP".equals(userState.getState())) {
                String text = messageText.trim().toLowerCase();
                String[] words = text.split(",");
                delFromTopTenList(chatId, words);
                getTopTenWordsList(chatId);

            } else if (messageText.startsWith(keywordsSearchText)) {
                new Thread(() -> findNewsByKeywords(chatId)).start();

            } else if (messageText.startsWith(updateTopText2)) {
                new Thread(() -> showTop(chatId)).start();

            } else if (messageText.startsWith(fullSearchText)) {
                new Thread(() -> findAllNews(chatId)).start();
            } else {
                /* Команды без параметров */
                switch (messageText) {
                    case "/start" -> startActions(update, chatId);
                    case "/settings" -> getSettings(chatId);
                    case "/search" -> initSearch(chatId);
                    case "/info" -> infoButtons(chatId);
                    case "/keywords" -> showKeywordsList(chatId);
                    case "/excluding" -> getExcludedList(chatId);
                    case "/delete" -> showYesNoOnDeleteUser(chatId);
                    case "/top" -> showTop(chatId);
                    default -> sendMessage(chatId, undefinedCommandText);
                }
            }
            userStates.remove(chatId);

            /* CALLBACK DATA */
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));

            switch (callbackData) {
                case "FEEDBACK" -> {
                    userStates.put(chatId, new UserState("SEND_FEEDBACK"));
                    cancelButton(chatId, sendMessageForDevText);
                }

                /* KEYWORDS */
                case "FIND_BY_KEYWORDS" -> new Thread(() -> findNewsByKeywords(chatId)).start();
                case "LIST_KEYWORDS" -> showKeywordsList(chatId);
                case "ADD" -> {
                    userStates.put(chatId, new UserState("ADD_KEYWORDS"));
                    cancelButton(chatId, addInListText);
                }
                case "DELETE" -> {
                    userStates.put(chatId, new UserState("DEL_KEYWORDS"));
                    cancelButton(chatId, delFromListText + "\n* - " + removeAllText);
                }

                case "START_SEARCH", "DELETE_NO" -> initSearch(chatId);

                /* FULL SEARCH */
                case "FIND_ALL" -> new Thread(() -> findAllNews(chatId)).start();

                /* EXCLUDED */
                case "LIST_EXCLUDED" -> getExcludedList(chatId);
                case "EXCLUDE" -> {
                    userStates.put(chatId, new UserState("ADD_EXCLUDED"));
                    cancelButton(chatId, addInListText);
                }
                case "DELETE_EXCLUDED" -> {
                    userStates.put(chatId, new UserState("DEL_EXCLUDED"));
                    cancelButton(chatId, delFromListText + "\n* - " + removeAllText);
                }

                /* TOP */
                case "DEL_FROM_TOP" -> deleteFromTopButtons(chatId);
                case "LIST_TOP" -> getTopTenWordsList(chatId);
                case "GET_TOP" -> showTop(chatId);
                case "WORD_SEARCH" -> new Thread(() -> topSearchButtons(chatId)).start();
                case "DELETE_TOP" -> {
                    userStates.put(chatId, new UserState("DEL_TOP"));
                    cancelButton(chatId, removeFromTopTenListText);
                }

                /* SETTINGS */
                case "GET_SETTINGS" -> getSettings(chatId);
                case "SET_PERIOD" -> getNumbersForKeywordsPeriodButtons(chatId);
                case "SET_PERIOD_ALL" -> getNumbersForAllPeriodButtons(chatId);
                case "SET_PERIOD_TOP" -> getNumbersForTopPeriodButtons(chatId);
                case "SET_EXCLUDED" -> showOnOffExcluded(chatId);
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

                case "JARO_WINKLER_MODE" -> showOnOffJaroWinkler(chatId);
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
                    nextButton(chatId, actionCanceledText);
                }

                // Language
                case "DE_BUTTON" -> setLang(chatId, "de");
                case "ES_BUTTON" -> setLang(chatId, "es");
                case "FR_BUTTON" -> setLang(chatId, "fr");
                case "RU_BUTTON" -> setLang(chatId, "ru");
                case "EN_BUTTON" -> setLang(chatId, "en");

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

                case "TOP_DEL_1" -> deleteWordFromTop(1, chatId);
                case "TOP_DEL_2" -> deleteWordFromTop(2, chatId);
                case "TOP_DEL_3" -> deleteWordFromTop(3, chatId);
                case "TOP_DEL_4" -> deleteWordFromTop(4, chatId);
                case "TOP_DEL_5" -> deleteWordFromTop(5, chatId);
                case "TOP_DEL_6" -> deleteWordFromTop(6, chatId);
                case "TOP_DEL_7" -> deleteWordFromTop(7, chatId);
                case "TOP_DEL_8" -> deleteWordFromTop(8, chatId);
                case "TOP_DEL_9" -> deleteWordFromTop(9, chatId);
                case "TOP_DEL_10" -> deleteWordFromTop(10, chatId);
                case "TOP_DEL_11" -> deleteWordFromTop(11, chatId);
                case "TOP_DEL_12" -> deleteWordFromTop(12, chatId);
                case "TOP_DEL_13" -> deleteWordFromTop(13, chatId);
                case "TOP_DEL_14" -> deleteWordFromTop(14, chatId);
                case "TOP_DEL_15" -> deleteWordFromTop(15, chatId);
                case "TOP_DEL_16" -> deleteWordFromTop(16, chatId);
                case "TOP_DEL_17" -> deleteWordFromTop(17, chatId);
                case "TOP_DEL_18" -> deleteWordFromTop(18, chatId);
                case "TOP_DEL_19" -> deleteWordFromTop(19, chatId);
                case "TOP_DEL_20" -> deleteWordFromTop(20, chatId);

                case "TOP_INTERVAL_1" -> updatePeriodTop(1, chatId);
                case "TOP_INTERVAL_4" -> updatePeriodTop(4, chatId);
                case "TOP_INTERVAL_8" -> updatePeriodTop(8, chatId);
                case "TOP_INTERVAL_12" -> updatePeriodTop(12, chatId);
                case "TOP_INTERVAL_24" -> updatePeriodTop(24, chatId);
                case "TOP_INTERVAL_48" -> updatePeriodTop(48, chatId);
                case "TOP_INTERVAL_72" -> updatePeriodTop(72, chatId);

                /* AUTO SEARCH BY KEYWORDS */
                case "SET_SCHEDULER" -> showOnOffScheduler(chatId);
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
                case "SET_START_0" -> updateSearchStartTime(0, chatId);
                case "SET_START_1" -> updateSearchStartTime(1, chatId);
                case "SET_START_2" -> updateSearchStartTime(2, chatId);
                case "SET_START_3" -> updateSearchStartTime(3, chatId);
                case "SET_START_4" -> updateSearchStartTime(4, chatId);
                case "SET_START_5" -> updateSearchStartTime(5, chatId);
                case "SET_START_6" -> updateSearchStartTime(6, chatId);
                case "SET_START_7" -> updateSearchStartTime(7, chatId);
                case "SET_START_8" -> updateSearchStartTime(8, chatId);
                case "SET_START_9" -> updateSearchStartTime(9, chatId);
                case "SET_START_10" -> updateSearchStartTime(10, chatId);
                case "SET_START_11" -> updateSearchStartTime(11, chatId);
                case "SET_START_12" -> updateSearchStartTime(12, chatId);
                case "SET_START_13" -> updateSearchStartTime(13, chatId);
                case "SET_START_14" -> updateSearchStartTime(14, chatId);
                case "SET_START_15" -> updateSearchStartTime(15, chatId);
                case "SET_START_16" -> updateSearchStartTime(16, chatId);
                case "SET_START_17" -> updateSearchStartTime(17, chatId);
                case "SET_START_18" -> updateSearchStartTime(18, chatId);
                case "SET_START_19" -> updateSearchStartTime(19, chatId);
                case "SET_START_20" -> updateSearchStartTime(20, chatId);
                case "SET_START_21" -> updateSearchStartTime(21, chatId);
                case "SET_START_22" -> updateSearchStartTime(22, chatId);
                case "SET_START_23" -> updateSearchStartTime(23, chatId);
            }
        }
    }

    private void updateSearchStartTime(int start, long chatId) {
        settingsRepository.updateStart(LocalTime.of(start, 0, 0), chatId);
        sendMessage(chatId, changesSavedText);
        getSettings(chatId);
    }

    private synchronized void removeKeywords(String keywords, long chatId) {
        ArrayList<String> words = new ArrayList<>();

        try {
            if (keywords.equals("*")) {
                words.add("*");
            } else {
                String[] nums = keywords.split(",");
                String[] split = getKeywordsList(chatId).split("\n");

                for (String num : nums) {
                    int numInt = Integer.parseInt(num.trim());

                    for (String row : split) {
                        int rowNum = Integer.parseInt(row.substring(0, row.indexOf(".")));
                        //row = row.replaceAll("\\s", "");
                        row = row.substring(row.indexOf(".") + 1);

                        if (numInt == rowNum) {
                            words.add(row);
                        }
                    }
                }
            }
            delKeyword(chatId, words);
            showKeywordsList(chatId);
        } catch (NumberFormatException n) {
            sendMessage(chatId, allowCommasAndNumbersText);
        } catch (NullPointerException npe) {
            sendMessage(chatId, "click /keywords" + TOP_TEN_SHOW_LIMIT);
        }
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
    }

    private void deleteWordFromTop(int wordNum, long chatId) {
        String word = "empty";

        try {
            String[] split = stringBuilderTop.toString().split("\n");

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
            String[] split = stringBuilderTop.toString().split("\n");

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
        sendMessage(chatId, changesSavedText);
        initSearch(chatId);
    }

    private void updatePeriodAll(int period, long chatId) {
        settingsRepository.updatePeriodAll(period + "h", chatId);
        sendMessage(chatId, changesSavedText);
        initSearch(chatId);
    }

    private void updatePeriodTop(int period, long chatId) {
        settingsRepository.updatePeriodTop(period + "h", chatId);
        sendMessage(chatId, changesSavedText);
        showTop(chatId);
    }

    private void removeUser(long chatId) {
        log.warn("User {} deleted account", userRepository.findById(chatId));
        userRepository.deleteById(chatId);
        String text = buyButtonText;
        sendMessage(chatId, text);
    }

    private String getRssList(long chatId) {
        String lang = settingsRepository.getLangByChatId(chatId);
        Iterable<RssList> sources = rssRepository.findAllActiveSources(lang);

        StringJoiner joiner = new StringJoiner(", ");
        for (RssList item : sources) {
            joiner.add(item.getSource());
        }

        return "<b>" + rssSourcesText + "</b>\n" + joiner;
    }

    private void startActions(Update update, long chatId) {
        addUser(update.getMessage());
        showLangButtons(chatId, "Select news language");
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
    }

    private void initSearch(long chatId) {
        String keywordsText = String.format("1" + initSearchTemplateText, keywordSearchText,
                settingsRepository.getPeriodByChatId(chatId), keywordRepository.getKeywordsCountByChatId(chatId),
                keywordSearch2Text);

        String fullText = String.format("2" + initSearchTemplateText, searchWithFilterText,
                settingsRepository.getPeriodAllByChatId(chatId), excludingTermsRepository.getExcludedCountByChatId(chatId),
                searchWithFilter2Text);

        String topText = String.format("3" + initSearchTemplateText, top20Text2,
                settingsRepository.getPeriodTopByChatId(chatId), topTenRepository.deleteFromTopTenCount(chatId),
                removedFromTopText);

        String text = "<b>" + searchNewsHeaderText + "</b> " + ICON_SEARCH +
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

    private void getSettings(long chatId) {
        Optional<Settings> sets = settingsRepository.findById(chatId).stream().findFirst();
        String lang = settingsRepository.getLangByChatId(chatId);

        sets.ifPresentOrElse(x -> {
                    x.setScheduler(setOnOffRus(x.getScheduler(), chatId));
                    x.setExcluded(setOnOffRus(x.getExcluded(), chatId));

                    String text = getSettingsText(x, lang);
                    getSettingsButtons(chatId, text);
                },
                () -> sendMessage(chatId, settingsNotFoundText)
        );
    }

    private void showKeywordsList(long chatId) {
        String joinerKeywords = getKeywordsList(chatId);

        if (joinerKeywords != null && joinerKeywords.length() != 0) {
            showKeywordButtons(chatId, "<b>" + listKeywordsText + "</b>\n" + joinerKeywords);
        } else {
            showAddKeywordsButton(chatId, setupKeywordsText);
        }
    }

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

    private void getExcludedList(long chatId) {
        List<String> excludedByChatId = excludingTermsRepository.findExcludedByChatId(chatId);
        int excludedCount = excludedByChatId.size();

        if (excludedByChatId.size() >= 400) {
            excludedByChatId = excludingTermsRepository.findExcludedByChatIdLimit(chatId, EXCLUDED_LIMIT);
        }

        if (!excludedByChatId.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            int counter = 0;
            for (String item : excludedByChatId) {
                joiner.add(item);
                if (++counter == EXCLUDING_TERMS_LIST_LIMIT) break;
            }
            showExcludedButtons(chatId, "<b>" + exclusionWordsText + "</b> [" + excludedCount + "]\n" +
                    joiner);
        } else {
            showExcludeButton(chatId);
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableHtml(true);
        message.disableWebPagePreview();
        executeMessage(message);
    }

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

    private void sendMessageWithPreview(long chatId, String textToSend, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (textToSend != null) message.setText(textToSend);
        else return;
        message.enableHtml(true);
        if (keyboard != null) message.setReplyMarkup(keyboard);
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
            log.warn(">>>  Добавлен новый пользователь: {}, {}, {}", user.getChatId(), user.getFirstName(),
                    user.getUserName());
            addSettings(message.getChatId());
        } else if (userRepository.isActive(message.getChatId()) == 0) {
            userRepository.updateIsActive(1, message.getChatId());
            log.warn(">>> Пользователь снова активен: {}, {}, {}", user.getChatId(), user.getFirstName(),
                    user.getUserName());
        }
    }

    private void addTopTen(long chatId, String word) {
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
    }

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
        settings.setJaroWinkler("on");
        settingsRepository.save(settings);
    }

    private void findAllNews(long chatId) {
        getReplyKeywordWithSearch(chatId, fullSearchStartText);
        //sendMessage(chatId, fullSearchStartText);

        Set<Headline> headlines = search.start(chatId, "all");

        int counterParts = 1;
        int showAllCounter = 1;
        if (headlines.size() > 0) {

            if (headlines.size() > LIMIT_FOR_BREAKING_INTO_PARTS) {
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
                    sleepBetweenSendMessage();

                    sendMessage(chatId, showAllCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                            Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                            headline.getTitle() + " " +
                            "<a href=\"" + headline.getLink() + "\">link</a>");
                }
            }

            nextButtonAfterAllSearch(chatId, foundNewsText + " <b>" + Search.totalNewsCounter + "</b> (" +
                    excludedNewsText + " <b>" + (Search.totalNewsCounter - headlines.size()) + "</b>) " +
                    ICON_NEWS_FOUNDED);
        } else {
            nextButtonAfterAllSearch(chatId, headlinesNotFound);
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
            getReplyKeywordWithSearch(chatId, searchByKeywordsStartText);
            //sendMessage(chatId, searchByKeywordsStartText);
        }

        // Search
        Set<Headline> headlines = search.start(chatId, "keywords");

        int showCounter = 1;
        if (headlines.size() > 0) {

            for (Headline headline : headlines) {
                String text = "<b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                if (!isAutoSearch.get()) {
                    text = showCounter++ + ". " + text;
                }

                sleepBetweenSendMessage();
                sendMessage(chatId, text);
            }

            if (!isAutoSearch.get()) {
                nextButtonAfterKeywordsSearch(chatId,
                        foundNewsText + ": <b>" + Search.filteredNewsCounter + "</b> " + ICON_NEWS_FOUNDED);
            }
        } else {
            if (!isAutoSearch.get()) {
                nextButtonAfterKeywordsSearch(chatId, headlinesNotFound);
            }
        }
        return Search.filteredNewsCounter;
    }

    private void wordSearch(long chatId, String word) {
        Set<Headline> headlines = search.start(chatId, word);

        int showCounter = 1;
        if (headlines.size() > 0) {
            for (Headline headline : headlines) {
                String text = showCounter++ + ". <b>" + headline.getSource() + "</b> [" +
                        Common.dateToShowFormatChange(String.valueOf(headline.getPubDate())) + "]\n" +
                        headline.getTitle() + " " +
                        "<a href=\"" + headline.getLink() + "\">link</a>";

                sleepBetweenSendMessage();
                sendMessage(chatId, text);

            }

            topSearchButtons(chatId);
        } else {
            showTopTenButton(chatId, headlinesNotFound);
        }
    }

    private static void sleepBetweenSendMessage() {
        try {
            Thread.sleep(SLEEP_BETWEEN_SENDING);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void sendFeedback(long chatId, String text) {
        String userName = userRepository.findNameByChatId(chatId);
        sendMessage(1254981379, "<b>Message</b> from user: " + userName + ", id: " + chatId + "\n" + text);
    }

    public void showOnOffScheduler(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SCHEDULER_OFF", "Off");
        buttons.put("SCHEDULER_ON", "On");
        sendMessage(chatId, autoSearchText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void showOnOffExcluded(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDED_OFF", "Off");
        buttons.put("EXCLUDED_ON", "On");
        sendMessage(chatId, exclusionText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void showOnOffJaroWinkler(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("JARO_WINKLER_OFF", "Off");
        buttons.put("JARO_WINKLER_ON", "On");

        String isActiveJw = settingsRepository.getJaroWinklerByChatId(chatId);
        isActiveJw = setOnOffRus(isActiveJw, chatId);

        sendMessage(chatId, jaroWinklerSwitcherText + "<b>" + isActiveJw +
                "</b>", InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Преобразование on/off в русские слова
    private String setOnOffRus(String value, long chatId) {
        String lang = settingsRepository.getLangByChatId(chatId);
        if (lang.equals("ru")) {
            value = value.equals("on") ? "вкл [on]" : "выкл [off]";
        }
        return value;
    }

    public void showYesNoOnStart(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("NO_BUTTON", noText);
        buttons.put("YES_BUTTON", yesText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void showLangButtons(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DE_BUTTON", "de");
        buttons.put("FR_BUTTON", "fr");
        buttons.put("ES_BUTTON", "es");
        buttons.put("RU_BUTTON", "ru");
        buttons.put("EN_BUTTON", "en");
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void showYesNoOnDeleteUser(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("DELETE_YES", yesText);
        buttons.put("DELETE_NO", noText);
        sendMessage(chatId, confirmDeletedUserText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    public void getNumbersForKeywordsPeriodButtons(long chatId) {
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

    public void getNumbersForAllPeriodButtons(long chatId) {
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

    public void getNumbersForTopPeriodButtons(long chatId) {
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

    public void topSearchButtons(long chatId) {
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
        sendMessage(chatId, chooseNumberWordFromTop, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
    }

    public void deleteFromTopButtons(long chatId) {
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

        sendMessage(chatId, chooseWordDelFromTop, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, buttons4, buttons5));
    }

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

    private void cancelButton(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("CANCEL", cancelButtonText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void showExcludedButtons(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_EXCLUDED", delText);
        buttons.put("EXCLUDE", addText);
        buttons.put("FIND_ALL", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void showExcludeButton(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("EXCLUDE", addText);
        sendMessage(chatId, excludedWordsNotSetText, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void showKeywordButtons(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE", delText);
        buttons.put("ADD", addText);
        buttons.put("FIND_BY_KEYWORDS", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void showAddKeywordsButton(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("ADD", addText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void getSettingsButtons(long chatId, String text) {
        boolean isOn = settingsRepository.getSchedulerOnOffByChatId(chatId).equals("on");

        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();
        buttons1.put("SET_SCHEDULER", "1. " + autoSearchText);
        buttons1.put("SET_PERIOD", "2. " + intervalText);
        buttons2.put("SET_EXCLUDED", "3. " + exclusionText);
        buttons2.put("SET_PERIOD_ALL", "4. " + intervalText);

        if (isOn) {
            buttons3.put("SCHEDULER_START", "5. " + startSettingsText);
            buttons3.put("START_SEARCH", "» » »");
        } else {
            buttons3.put("START_SEARCH", "» » »");
        }

        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
    }

    private void nextButton(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("START_SEARCH", "» » »");
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void infoButtons(long chatId) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("FEEDBACK", sendIdeaText);
        buttons.put("START_SEARCH", "» » »");
        sendMessageWithPreview(chatId, aboutDeveloperText + "\n\n" + getRssList(chatId),
                InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void nextButtonAfterKeywordsSearch(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD", intervalText);
        buttons.put("LIST_KEYWORDS", listKeywordsText);
        buttons.put("FIND_BY_KEYWORDS", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    private void nextButtonAfterAllSearch(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("SET_PERIOD_ALL", intervalText);
        buttons.put("EXCLUDE", excludeWordText);
        buttons.put("FIND_ALL", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Показать топ слов за период
    private void showTopTenButtons(long chatId, String text) {
        Map<String, String> buttons1 = new LinkedHashMap<>();
        Map<String, String> buttons2 = new LinkedHashMap<>();
        Map<String, String> buttons3 = new LinkedHashMap<>();

        buttons1.put("LIST_TOP", excludedListText);
        buttons1.put("DEL_FROM_TOP", delFromTopText);
        buttons2.put("SET_PERIOD_TOP", intervalText);
        buttons2.put("GET_TOP", updateTopText);
        buttons3.put("JARO_WINKLER_MODE", jaroWinklerText);
        buttons3.put("WORD_SEARCH", searchText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons1, buttons2, buttons3, null, null));
    }

    private void showTopTenButton(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();
        buttons.put("GET_TOP", updateTopText);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
    }

    // Список удалённых слов из топа
    private void showTopTenListButtons(long chatId, String text) {
        Map<String, String> buttons = new LinkedHashMap<>();

        buttons.put("DELETE_TOP", delText);
        buttons.put("DEL_FROM_TOP", addText);
        buttons.put("GET_TOP", updateTopText2);
        sendMessage(chatId, text, InlineKeyboards.inlineKeyboardMaker(buttons));
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
            stringBuilderTop = new StringBuilder();
            String point = ".    ";

            for (String s : topTen) {
                if (x >= 10) point = ".  ";
                stringBuilderTop.append(x++).append(point).append(s);
            }

            String period = settingsRepository.getPeriodTopByChatId(chatId);

            showTopTenButtons(chatId, String.format("%s<b>%s</b> " + ICON_TOP_20_FIRE + " \n%s", top20ByPeriodText,
                    period, stringBuilderTop));
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

        wordsCount = wordsCount.entrySet().stream()
                .filter(x -> x.getValue() > 3)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        if ("on".equals(settingsRepository.getJaroWinklerByChatId(chatId))) {
            wordsCount = Common.fillTopWithoutDuplicates(wordsCount, JARO_WINKLER_LEVEL);
        }

        // Удаление исключённых слов из мап для анализа
        Set<String> excludedWordsFromAnalysis = topTenRepository.findAllExcludedFromTopTenByChatId(chatId);
        for (String word : excludedWordsFromAnalysis) {
            wordsCount.remove(word);
        }

        return wordsCount.entrySet().stream()
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
    }

    private void setLang(long chatId, String lang) {
        setInterfaceLanguage(lang);
        settingsRepository.updateLanguage(lang, chatId);
        setInterfaceLanguage(settingsRepository.getLangByChatId(chatId));
        getReplyKeywordWithSearch(chatId, greetingText, userRepository.findNameByChatId(chatId));
        showYesNoOnStart(chatId, letsStartText);
        createMenuCommands();
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

    private void getReplyKeywordWithSearch(long chatId, String textToSend, String firstName) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(keywordsSearchText);
        row.add(fullSearchText);
        row.add(updateTopText2);

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage(chatId, String.format(textToSend, firstName), keyboardMarkup);
    }

    private void getReplyKeywordWithSearch(long chatId, String textToSend) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(keywordsSearchText);
        row.add(fullSearchText);
        row.add(updateTopText2);

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage(chatId, textToSend, keyboardMarkup);
    }

    private void createMenuCommands() {
        List<BotCommand> listOfCommands = new LinkedList<>();
        listOfCommands.add(new BotCommand("/settings", settingText));
        //listOfCommands.add(new BotCommand("/excluding", listExcludedText));
        //listOfCommands.add(new BotCommand("/keywords", listKeywordsText));
        listOfCommands.add(new BotCommand("/search", findSelectText));
        listOfCommands.add(new BotCommand("/info", infoText));
        listOfCommands.add(new BotCommand("/top", top20Text));
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