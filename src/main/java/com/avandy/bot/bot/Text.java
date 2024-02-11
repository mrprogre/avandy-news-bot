package com.avandy.bot.bot;

import com.avandy.bot.model.Settings;
import com.avandy.bot.utils.Common;
import com.vdurmont.emoji.EmojiParser;

public class Text extends Common {
    private static final String delimiterNews = "- - - - - - - - - - - - - - - - - - - - - - - -";
    public static String
            greetingText, letsStartText, noText, yesText, infoText, settingText, listKeywordsText, findSelectText,
            top20Text, top20Text2, listExcludedText, deleteUserText, addText, delText, excludeWordText, searchText,
            excludedText, listKeywordsButtonText, updateTopText, updateTopText2, undefinedCommandText, changesSavedText,
            jaroWinklerSwitcherText, headlinesNotFound, setupKeywordsText, minWordLengthText, deleteAllWordsText,
            jaroWinklerText, wordIsExistsText, wordsIsNotAddedText, wordsAddedText, aboutDeveloperText, addText2,
            initSearchTemplateText, yesButtonText, buyButtonText, intervalText, autoSearchText, exclusionText,
            startSettingsText, excludedListText, delFromTopText, top20ByPeriodText, allowCommasAndNumbersText,
            premiumText, startSearchBeforeText, keywordsSearchText, fullSearchText, chooseSearchStartText,
            getPremiumText, addInListText, delFromListText, removeAllText, sendMessageForDevText, searchNewsHeaderText,
            actionCanceledText, removeFromTopTenListText, rssSourcesText, fullSearchStartText, chooseNumberWordFromTop,
            searchWithFilterText, searchWithFilter2Text, keywordSearchText, keywordSearch2Text, cancelButtonText,
            foundNewsText, excludedNewsText, settingsNotFoundText, exclusionWordsText, addedExceptionWordsText,
            wordIsNotInTheListText, listText, excludedWordsNotSetText, confirmDeletedUserText, chooseWordDelFromTop,
            removedFromTopText, chooseSearchDepthText, sendIdeaText, listOfDeletedFromTopText,
            getPremiumYesOrNowText, getPremiumRequestText, premiumIsActive, premiumIsActive2, excludeWordText2,
            premiumIsActive3, premiumIsActive4, inputExceptionText, premiumSearchSettingsText, inputExceptionText2,
            sentText, searchByKeywordsStartText, sendPaymentText, sendMessageForDevText2, cleanFullSearchHistoryText,
            historyClearText, advancedSearch, addText3, addSourceText, delSourceText, addSourceInfoText,
            premiumAddSourcesText, notSupportedRssText, rssExistsText, delSourceInfoText, deleteText,
            rssNameNotExistsText, linkText;

    public static void setInterfaceLanguage(String lang) {
        if (lang != null && lang.equals("ru")) {
            initSearchTemplateText = ". %s%s\n[Список: <b>%d</b> %s]";
            searchNewsHeaderText = "Поиск новостей";
            greetingText = EmojiParser.parseToUnicode("Здравствуйте! :blush: \n" +
                    "Я могу найти для Вас важную информацию и отсеять много лишней!");
            letsStartText = "Продолжить?";
            noText = "Нет";
            yesText = "Да";
            infoText = "Информация и подписка";
            settingText = "Настройки";
            listKeywordsButtonText = "Ключевые слова";
            findSelectText = "Выбрать тип поиска";
            top20Text = "Часто употребляемые слова";
            top20Text2 = "<b>Top 20</b> слов за ";
            listExcludedText = "Слова-исключения";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            addText2 = "Добавить ключевые слова";
            addText3 = "Добавить слова";
            delText = "Удалить";
            excludeWordText = "Исключить";
            excludeWordText2 = "Добавить слова-исключения";
            searchText = "Поиск";
            excludedText = "Исключённое";
            updateTopText = "Обновить";
            updateTopText2 = "Top 20";
            undefinedCommandText = "Данная команда не существует, либо Вы не нажали кнопку перед вводом текста " +
                    ICON_SMILE_UPSIDE_DOWN;
            changesSavedText = "Сохранено ✔️";
            sentText = "Доставлено ✔️";
            historyClearText = "История просмотра очищена ✔️";
            headlinesNotFound = "» новостей нет " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» ключевые слова не заданы";
            minWordLengthText = "Длина слова должна быть более 1 символа и менее 18";
            deleteAllWordsText = "❌ все слова удалены";
            deleteText = "❌ удалено";
            wordIsExistsText = "Слово уже есть в списке: ";
            wordsIsNotAddedText = "Слова не добавлены";
            wordsAddedText = "Добавлено слов";
            aboutDeveloperText = """
                    <b>Разработчик</b>: <a href="https://github.com/mrprogre">mrprogre</a>
                    <b>Почта</b>: rps_project@mail.ru
                    <b>Основная программа:</b> <a href="https://avandy-news.ru">Avandy News Analysis</a> (запись в Реестре российского ПО: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)
                    <b>Тестировщики:</b> <a href="https://www.instagram.com/andrew.sereda.1968?utm_source=qr&igsh=MWV1Z2pxZDN6Mm8ycA==">Andrew Sereda</a>
                    <b>Видеоинструкция:</b> <a href="https://youtu.be/HPAk8GPHes4">youtube</a>""";
            buyButtonText = "Пока, друг! " + ICON_GOOD_BYE;
            intervalText = "Глубина";
            autoSearchText = "Автопоиск";
            exclusionText = "Исключение";
            startSettingsText = "Старт";
            excludedListText = "Удалённое";
            delFromTopText = "Удалить";
            top20ByPeriodText = "<b>Top 20 слов</b> за ";
            allowCommasAndNumbersText = "Укажите порядковый номер слова (разделять запятой)";
            startSearchBeforeText = "Топ необходимо обновить. Нажмите на /top";
            keywordsSearchText = "По словам";
            fullSearchText = "Полный";
            addInListText = "Если слов несколько, то раздели их запятыми";
            delFromListText = "Введите <b>порядковый номер</b> слова, а если их несколько, то раздели запятыми";
            removeAllText = "удалить всё";
            sendMessageForDevText = "Написать сообщение разработчику (можно приложить один скриншот)";
            sendMessageForDevText2 = "Направить скриншот оплаты";
            actionCanceledText = "Действие отменено";
            removeFromTopTenListText = "Введите слова для удаления (разделять запятой)";
            rssSourcesText = "Источники новостей";
            searchWithFilterText = "<b>Полный поиск</b> за ";
            searchWithFilter2Text = "слов-исключений";
            keywordSearchText = "По <b>ключевым словам</b> за ";
            keywordSearch2Text = "ключевых слов";
            cancelButtonText = "Отменить";
            foundNewsText = "Найдено";
            excludedNewsText = "исключено";
            settingsNotFoundText = "Настройки не обнаружены";
            excludedWordsNotSetText = "» слова-исключения не заданы";
            exclusionWordsText = "Слова-исключения";
            addedExceptionWordsText = "Добавлено слов-исключений";
            wordIsNotInTheListText = "Слово %s отсутствует в списке";
            listText = "Список";
            confirmDeletedUserText = "Подтверждаете удаление пользователя?";
            chooseSearchDepthText = "Глубина поиска новостей (интервал поиска равен текущему моменту минус глубина в часах).\n" +
                    "Выберите глубину поиска.";
            sendIdeaText = "Написать отзыв";
            listOfDeletedFromTopText = "Список удалённого из Top 20";
            fullSearchStartText = "» поиск всех новостей";
            searchByKeywordsStartText = "» ручной поиск по ключевым словам за 48 часов";
            chooseNumberWordFromTop = "Нажмите для поиска";
            chooseWordDelFromTop = "Нажмите для удаления";
            chooseSearchStartText = "Выберите час для расчёта времени запуска автопоиска по ключевым словам";
            removedFromTopText = "удалено из Топа";
            jaroWinklerSwitcherText = """
                    Чтобы объединить одинаковые слова для исключения повторений можно включить удаление окончаний.
                    Пример: в Top 20 показаны слова: <b>белгородская [10], белгорода [7], белгороде [4]</b>, при включённом удалении будет показано только одно слово <b>белгород [21]</b> c общей суммой ранее указанных слов.
                    Текущий статус:\s""";
            jaroWinklerText = "Окончания";
            premiumText = " (для <b>премиум аккаунта</b> запуск поиска производится <b>каждые 2 минуты</b> /premium) " +
                    ICON_PREMIUM_IS_ACTIVE;
            getPremiumText = "Премиум бот";
            getPremiumYesOrNowText = "На тарифе <b>Premium</b>\n" +
                    "1. Поиск новостей по ключевым словам производится каждые <b>2 минуты</b>. " +
                    "Так Вы <b>первым</b> будете получать <b>самую актуальную информацию</b>;\n" +
                    "2. Можно добавить <b>более " + MAX_KEYWORDS_COUNT + "</b> ключевых слов (до " +
                    MAX_KEYWORDS_COUNT_PREMIUM + ");\n" +
                    "3. Возможность добавления <b>персональных источников</b> новостей " +
                    "(если источник предоставляет новости в формате <b>XML</b>) и удаления источников по умолчанию.\n" +
                    "Оплатить премиум <b>900р.</b> в год? \uD83D\uDC8E";
            getPremiumRequestText = """
                    1. <a href="https://qr.nspk.ru/AS1A0047UDL2QR108KLRR4844KTLRSH6?type=01&bank=100000000284&crc=D835">Ссылка на оплату через СБП</a>.
                    В ближайшие 12 часов после отправки подтверждения платежа Вам будут подключены премиум возможности бота.
                    2. <a href="https://avandy-news.ru/refund.html">Возврат средств</a>
                    Не подошёл наш продукт и прошло <b>не более 3 дней</b> с момента активации премиум возможностей - напишите, и мы <b>вернём</b> средства!""";
            premiumIsActive = "активирован премиум " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive2 = "Premium активирован до %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive3 = "Добавление более " + MAX_KEYWORDS_COUNT + " ключевых слов возможно в <b>/premium</b> режиме";
            premiumIsActive4 = "Максимальное количество ключевых слов - " + MAX_KEYWORDS_COUNT_PREMIUM;
            yesButtonText = """
                    Добавьте <b>слова</b> по которым я буду искать новости для Вас
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Нажмите кнопку <b>Добавить</b> или посмотрите о чём сейчас пишут СМИ, нажав на <b>Top 20</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    <b>Видеоинструкция</b> на <a href="https://youtu.be/HPAk8GPHes4">youtube</a>""";
            inputExceptionText = "Бот ожидал ввода текста. Для продолжения работы нажмите повторно ";
            inputExceptionText2 = "Бот ожидал ввода текста. Для продолжения работы повторите команду";
            premiumSearchSettingsText = "Премиум поиск";
            sendPaymentText = "Отправить скриншот с оплатой";
            cleanFullSearchHistoryText = "очистить историю просмотра /clear";
            advancedSearch = """
                    Для получения большего количества результатов при меньшем количестве слов
                    используйте символ <b>*</b> вместо любой буквы в слове.
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    <b>К примеру,</b> ключевое слово <b>курс</b> найдёт новости содержащие только это слово без окончаний
                    <b>курс*</b> найдёт как <b>курс</b>, так и <b>курсы</b>
                    <b>курс**</b> найдёт <b>курс, курсы, курсах</b>, но новостей с <b>курсами</b> не найдёт, т.к. надо указать три звёздочки
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Можно искать новости <b>по фразам</b>:
                    Ключевая фраза <b>Росси* созда**</b> найдёт новости содержащие: в <b>России создана, Россия создала</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Перейти в раздел /keywords""";
            listKeywordsText = """
                    <b>Ключевые слова</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Автопоиск %s
                    Запуск поиска раз в %s
                    Продвинутый поиск /advanced
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    %s""";
            addSourceText = "Добавить источник";
            delSourceText = "Удалить источник";
            addSourceInfoText = "Введите <b>страну</b>, <b>название</b> источника и <b>веб ссылку на xml</b> c новостями, разделяя всё запятой";
            delSourceInfoText = "Удалить <b>персональный</b> источник по его названию";
            premiumAddSourcesText = "Добавление/удаление <b>персональных</b> источников возможно в /premium\n" +
                    "<b>Огромный список</b> источников по категориям и странам: <a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            notSupportedRssText = "Данный источник не поддерживается";
            rssExistsText = "Данный источник уже есть в списке";
            rssNameNotExistsText = "Источника с таким названием не существует";
            linkText = "Источник";
        } else {
            linkText = "link";
            rssNameNotExistsText = "There is no source with this name";
            rssExistsText = "This source is already in the list";
            notSupportedRssText = "This source is not supported";
            premiumAddSourcesText = "Adding/removing <b>personal</b> sources is possible in /premium\n" +
                    "<b>Huge list</b> of sources by category and country: <a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            addSourceInfoText = "Enter the <b>country</b>, <b>source name</b> and <b>xml link</b>, separated all by commas";
            delSourceInfoText = "Delete <b>your</b> source by its name";
            addSourceText = "Add news source";
            delSourceText = "Delete news source";
            listKeywordsText = """
                    <b>Keywords</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Auto search %s
                    The search runs every <b>%s</b>
                    Advanced search /advanced
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    %s""";
            advancedSearch = """
                    However you can use the <b>*</b> symbol
                    1. <b>tax</b> will find all news only with <b>tax</b> but not <b>taxes, taxcom</b>.
                    2. <b>tax**</b> will find <b>tax, taxi, taxes</b> but not <b>taxcom</b>.
                    3. <b>pay tax**</b> will find headlines with <b>pay taxes</b>.
                    Go to /keywords section""";
            cleanFullSearchHistoryText = "clear browsing history /clear";
            sendPaymentText = "Send transfer confirmation";
            premiumSearchSettingsText = "Premium search";
            inputExceptionText = "The bot was waiting for text to be entered. To continue, click again ";
            inputExceptionText2 = "The bot was waiting for text input. To continue, repeat the command";
            yesButtonText = """
                    The first thing to do is add <b>keywords</b>.
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    You can also watch the <b>video</b> on <a href="https://youtu.be/HPAk8GPHes4">youtube</a>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Click the <b>Add keywords</b> button or see what the media are writing about now by clicking on <b>Top 20</b>""";
            premiumIsActive4 = "Maximum number of keywords - " + MAX_KEYWORDS_COUNT_PREMIUM;
            premiumIsActive3 = "Adding more than " + MAX_KEYWORDS_COUNT + " is possible in <b>/premium</b> mode";
            premiumIsActive2 = "Premium activated until %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive = "premium is active " + ICON_PREMIUM_IS_ACTIVE;
            getPremiumRequestText = "<a href=\"paypal.me/avandyelectronics\">Paypal link</a>";
            getPremiumYesOrNowText = "On the <b>premium</b> tariff:\n" +
                    "1. News search by keywords is performed every <b>2 minutes</b>. " +
                    "So you <b>will be the first</b> to receive the <b>most up-to-date information</b>;\n" +
                    "2. You can add <b>more than " + MAX_KEYWORDS_COUNT + "</b> keywords (up to " +
                    MAX_KEYWORDS_COUNT_PREMIUM + " words);\n" +
                    "3. Ability to add <b>personal news sources</b> (if the source provides <b>XML</b> format) " +
                    "and removing default sources.\n" +
                    "Pay for a Premium subscription <b>50$</b> per year \uD83D\uDC8E";
            getPremiumText = "Premium";
            premiumText = " (for a <b>premium account</b>, the search starts <b>every 2 minutes</b> /premium) " +
                    ICON_PREMIUM_IS_ACTIVE;
            initSearchTemplateText = ". %s%s\n[List: <b>%d</b> %s]";
            searchNewsHeaderText = "Search news";
            greetingText = EmojiParser.parseToUnicode("Hello, %s! :blush: \n" +
                    "I can find important information for you and hide a lot of unnecessary information!");
            letsStartText = "Continue?";
            noText = "No";
            yesText = "Yes";
            infoText = "App info and premium";
            settingText = "Settings";
            listKeywordsButtonText = "Keywords";
            findSelectText = "Search types";
            top20Text = "Frequently used words by period";
            top20Text2 = "<b>Top 20</b> words in ";
            listExcludedText = "Excluding terms";
            deleteUserText = "Delete user data";
            addText = "Add";
            addText2 = "Add keywords";
            addText3 = "Add keywords";
            delText = "Delete";
            excludeWordText = "Exclude term";
            excludeWordText2 = "Add excluding terms";
            searchText = "Search";
            excludedText = "Excluded";
            undefinedCommandText = "This command doesn't exist or you did not click the button before entering text! " +
                    "However, I will see your message in the journal " + ICON_SMILE_UPSIDE_DOWN;
            changesSavedText = "Done ✔️";
            sentText = "Done ✔️";
            historyClearText = "Browsing history cleared ✔️";
            headlinesNotFound = "» no news headlines found " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» no keywords specified";
            minWordLengthText = "Word length must be more than 1 characters and less than 18";
            deleteAllWordsText = "❌ all words removed";
            deleteText = "❌ deleted";
            wordIsExistsText = "The word is already on the list: ";
            wordsIsNotAddedText = "No words added";
            wordsAddedText = "Word added";
            aboutDeveloperText = """
                    <b>Developer</b>: <a href="https://github.com/mrprogre">mrprogre</a>
                    <b>E-mail</b>: rps_project@mail.ru
                    <b>Main application:</b> <a href="https://avandy-news.ru/index-en.html">Avandy News Analysis</a> (entry in the Register of Russian Software No: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)
                    <b>Testing:</b> <a href="https://www.instagram.com/andrew.sereda.1968?utm_source=qr&igsh=MWV1Z2pxZDN6Mm8ycA==">Andrew Sereda</a>
                    <b>Video:</b> <a href="https://youtu.be/HPAk8GPHes4">youtube</a>
                    """;
            buyButtonText = "Good buy! " + ICON_GOOD_BYE;
            intervalText = "Interval";
            autoSearchText = "Auto search";
            exclusionText = "News exclusion";
            startSettingsText = "Start";
            excludedListText = "List of deleted";
            delFromTopText = "Delete";
            updateTopText = "Update top";
            updateTopText2 = "Show top";
            top20ByPeriodText = "<b>Top 20 words</b> in ";
            allowCommasAndNumbersText = "Only numbers or commas are allowed";
            startSearchBeforeText = "First press /top";
            keywordsSearchText = "Keywords";
            fullSearchText = "Full search";
            addInListText = "Input words to add (separated by comma) and send the message";
            delFromListText = "Input word numbers to remove (separated by comma)";
            removeAllText = "delete all";
            sendMessageForDevText = "Write a message to the developer (you can attach one screenshot)";
            sendMessageForDevText2 = "Send a screenshot of payment";
            actionCanceledText = "Cancelled";
            removeFromTopTenListText = "Enter words to delete (separated by comma)";
            rssSourcesText = "News sources";
            searchWithFilterText = "<b>Full search</b> in ";
            searchWithFilter2Text = "excluding terms";
            keywordSearchText = "Search by <b>keywords</b> in ";
            keywordSearch2Text = "keywords";
            cancelButtonText = "Cancel";
            foundNewsText = "Found";
            excludedNewsText = "excluded";
            settingsNotFoundText = "Settings not found";
            excludedWordsNotSetText = "» no excluding terms specified";
            exclusionWordsText = "Excluding terms";
            addedExceptionWordsText = "Added excluding terms";
            wordIsNotInTheListText = "The word <b>%s</b> isn't in the list";
            listText = "List";
            confirmDeletedUserText = "Do you confirm?";
            chooseSearchDepthText = "Search time = current moment minus hours. Select time.";
            sendIdeaText = "Send feedback";
            listOfDeletedFromTopText = "List of deleted from the Top";
            fullSearchStartText = "» search all news";
            searchByKeywordsStartText = "» search by keywords";
            chooseNumberWordFromTop = "Click to search";
            chooseWordDelFromTop = "Click to delete";
            chooseSearchStartText = "Select an hour to calculate the time to start auto search by keywords";
            removedFromTopText = "words removed";
            jaroWinklerText = "Word endings";
            jaroWinklerSwitcherText = """
                    To combine identical words to eliminate repetitions, you can enable removal of word endings
                    For example: "played, player" will be combined into one word "play".
                    Current status:\s""";
        }
    }

    public static String getSettingsText(Settings settings, String lang, int isPremium) {
        String premiumPeriod;
        String premiumMessage;
        String premiumSettings = "";

        if (lang.equals("ru")) {
            String schedSettings = "";
            if (settings.getScheduler().contains("on") && isPremium != 1) {
                String text = switch (settings.getPeriod()) {
                    case "1h" -> " (запуск каждый час)\n";
                    case "2h" -> " (запуск каждые 2 часа)\n";
                    case "24h", "48h", "72h" -> " (запуск один раз в сутки)\n";
                    default ->
                            " (часы запуска: <b>" + getTimeToExecute(settings.getStart(), settings.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = delimiterNews + "\n<b>5. Старт</b> автопоиска: <b>" + settings.getStart() + "</b>" + text;
            }

            if (isPremium == 1) {
                String period = settings.getPremiumSearch().equals("on") ? "2 min" : settings.getPeriod();
                premiumPeriod = "<b>" + period + "</b>, " + premiumIsActive;
                premiumMessage = "";
                String rus = settings.getPremiumSearch().equals("on") ? "<b>вкл [on]</b>" : "<b>выкл [off]</b>";
                premiumSettings = delimiterNews + "\n<b>5. Премиум поиск:</b> " + rus + "\n" +
                        "Включить или выключить автопоиск каждые 2 минуты (будет запускаться в соответствии с п.2)\n";
            } else {
                premiumPeriod = settings.getPeriod();
                premiumMessage = premiumText;
            }

            return "<b>Настройки</b> " + ICON_SETTINGS + " \n" +
                    "<b>1. Автопоиск: " + settings.getScheduler() + "</b>\n" +
                    "Автоматический запуск поиска по <b>ключевым словам</b>\n" +
                    delimiterNews + "\n" +
                    "<b>2. Частота автопоиска:</b> " + premiumPeriod + "\n" +
                    "Глубина поиска совпадает с частотой и равна текущему времени минус <b>" + settings.getPeriod() + "</b>" +
                    premiumMessage + "\n" +
                    delimiterNews + "\n" +
                    "<b>3. Исключение</b>: <b>" + settings.getExcluded() + "</b>\n" +
                    "Исключение новостей, которые содержат слова-исключения\n" +
                    delimiterNews + "\n" +
                    "<b>4. Глубина полного поиска</b>\n" +
                    "Глубина равна текущему времени минус <b>" + settings.getPeriodAll() + "</b>\n" +
                    schedSettings +
                    premiumSettings +
                    delimiterNews + "\n" +
                    "Параметры меняются после нажатия на кнопки";
        } else {
            String schedSettings = "";

            if (settings.getScheduler().contains("on") && isPremium != 1) {
                String text = switch (settings.getPeriod()) {
                    case "1h" -> " (launch every hour)\n";
                    case "2h" -> " (launch every 2 hours)\n";
                    case "24h", "48h", "72h" -> " (launch once a day)\n";
                    default ->
                            " (start time: <b>" + getTimeToExecute(settings.getStart(), settings.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = delimiterNews + "\n" + "<b>5. Start</b> auto search by keywords: <b>" + settings.getStart() + "</b>" + text;
            }

            if (isPremium == 1) {
                String period = settings.getPremiumSearch().equals("on") ? "2 min" : settings.getPeriod();
                premiumPeriod = "<b>" + period + "</b>, " + premiumIsActive;
                premiumMessage = "";
                premiumSettings = delimiterNews + "\n<b>5. Premium search: " + settings.getPremiumSearch() + "</b>\n" +
                        "Enable or disable auto search every 2 minutes (will be launched in accordance with step 2)\n";
            } else {
                premiumPeriod = settings.getPeriod();
                premiumMessage = premiumText;
            }

            return "<b>Settings</b> " + ICON_SETTINGS + " \n" +
                    "<b>1. Auto search: " + settings.getScheduler() + "</b>\n" +
                    "Automatically launch a search using <b>keywords</b> for the period specified in clause 1, with a frequency in clause 5" + "\n" +
                    delimiterNews + "\n" +
                    "<b>2. Auto search frequency: " + premiumPeriod + "</b>\n" +
                    "The search period coincides with the search frequency and is equal to the current time minus <b>" + settings.getPeriod() + "</b>" +
                    premiumMessage + "\n" +
                    delimiterNews + "\n" +
                    "<b>3. Excluding</b>: <b>" + settings.getExcluded() + "</b>\n" +
                    "Excluding news that contains excluding terms\n" +
                    delimiterNews + "\n" +
                    "<b>4. Full search interval: " + settings.getPeriodAll() + "</b>\n" +
                    schedSettings +
                    premiumSettings +
                    delimiterNews + "\n" +
                    "Parameters change after pressing buttons";
        }
    }

}