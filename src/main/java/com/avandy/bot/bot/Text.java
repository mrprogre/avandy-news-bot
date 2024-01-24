package com.avandy.bot.bot;

import com.avandy.bot.model.Settings;
import com.avandy.bot.utils.Common;
import com.vdurmont.emoji.EmojiParser;

public class Text extends Common {
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
            removedFromTopText, chooseSearchDepthText, sendIdeaText, listOfDeletedFromTopText, searchByKeywordsStartText,
            getPremiumYesOrNowText, getPremiumRequestText, premiumIsActive, premiumIsActive2, excludeWordText2,
            premiumIsActive3;

    public static void setInterfaceLanguage(String lang) {
        if (lang != null && lang.equals("ru")) {
            initSearchTemplateText = ". %s%s\n[Список: <b>%d</b> %s]";
            searchNewsHeaderText = "Поиск новостей";
            greetingText = EmojiParser.parseToUnicode("Здравствуй, %s! :blush: \n" +
                    "Я могу найти для тебя важную информацию и отсеять много лишней!");
            letsStartText = "Продолжим?";
            noText = "Нет";
            yesText = "Да";
            infoText = "Информация и подписка";
            settingText = "Настройки";
            listKeywordsText = """
                    <b>Ключевые слова</b>
                    - - - - - -
                    Для гибкости можно использовать символ *, который обозначает одну любую букву. Так, например, ключевое слово <b>курс**</b> найдёт новости со словами <b>курс, курсы, курсом</b>, но не найдёт массу лишних новостей о <b>Курской</b> области. Однако найдёт, если сделаете три звезды <b>курс***</b>
                    - - - - - -""";
            listKeywordsButtonText = "Ключевые слова";
            findSelectText = "Выбрать тип поиска";
            top20Text = "Часто употребляемые слова";
            top20Text2 = "<b>Top 20</b> слов за ";
            listExcludedText = "Слова-исключения";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            addText2 = "Добавить ключевые слова";
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
            headlinesNotFound = "» новости не найдены " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» ключевые слова не заданы";
            minWordLengthText = "Длина слова должна быть более 2 символов";
            deleteAllWordsText = "❌ все слова удалены";
            wordIsExistsText = "Слово уже есть в списке: ";
            wordsIsNotAddedText = "Слова не добавлены";
            wordsAddedText = "Слово добавлено";
            aboutDeveloperText = """
                    <b>Разработчик</b>: <a href="https://github.com/mrprogre">mrprogre</a>
                    <b>Почта</b>: rps_project@mail.ru
                    <b>Основная программа:</b> <a href="https://avandy-news.ru">Avandy News Analysis</a> (запись в Реестре российского ПО: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""";
            yesButtonText = """
                    Первое, что нужно сделать, - добавить <b>ключевые слова</b>. Если интересуют новости, связанные с ипотекой, то добавь слово со звёздочкой <b>ипотек*</b>, чтобы получить все заголовки, содержащие: <b>ипотекА, ипотекУ</b>. Одна звездочка это одна любая буква.
                    - - - - - -
                    Нажми кнопку "Добавить".""";
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
            addInListText = "Введите слова для добавления в список (разделять запятой) и нажмите \"Отправить\"";
            delFromListText = "Укажите номера слов для удаления (разделять запятой)";
            removeAllText = "удалить всё";
            sendMessageForDevText = "Напишите разработчику";
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
            sendIdeaText = "Отзыв";
            listOfDeletedFromTopText = "Список удалённого из Top 20";
            searchByKeywordsStartText = "» поиск по ключевым словам";
            fullSearchStartText = "» поиск всех новостей";
            chooseNumberWordFromTop = "Нажмите для поиска";
            chooseWordDelFromTop = "Нажмите для удаления";
            chooseSearchStartText = "Выберите час для расчёта времени запуска автопоиска по ключевым словам";
            removedFromTopText = "удалено из Топа";
            jaroWinklerSwitcherText = """
                    Чтобы объединить одинаковые слова для исключения повторений можно включить удаление окончаний.
                    Пример: в Top 20 показаны слова: <b>белгородская [10], белгорода [7], белгороде [4]</b>, при включённом удалении будет показано только одно слово <b>белгород [21]</b> c общей суммой ранее указанных слов.
                    Текущий статус:\s""";
            jaroWinklerText = "Окончания";
            premiumText = " (для <b>премиум аккаунта</b> запуск поиска производится <b>каждые 2 минуты</b>) " + ICON_PREMIUM_IS_ACTIVE;
            getPremiumText = "Премиум";
            getPremiumYesOrNowText = """
                    На тарифе <b>Premium</b>
                    1. Поиск новостей по ключевым словам производится <b>каждые 2 минуты</b>. Так Вы <b>первым</b> будете получать <b>самую актуальную информацию</b>.
                    2. Возможность добавить <b>более 10</b> ключевых слов.
                    Оплатить премиум (<b>900р.</b> в год)? \uD83D\uDC8E
                    """;
            getPremiumRequestText = "<a href=\"https://qr.nspk.ru/AS1A0047UDL2QR108KLRR4844KTLRSH6?type=01&bank=100000000284&crc=D835\">1. Оплата через СБП (всё официально, мы платим налоги)</a>\n" +
                    "<a href=\"https://avandy-news.ru/refund.html\">2. Правила возврата средств</a>";
            premiumIsActive = "активирован премиум " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive2 = "Premium активирован до %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive3 = "Добавление более 10 ключевых слов возможно в <b>Premium</b> режиме";
        } else {
            premiumIsActive3 = "Adding more than 10 keywords is possible in <b>Premium</b> mode";
            premiumIsActive2 = "Premium activated until %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive = "premium is active " + ICON_PREMIUM_IS_ACTIVE;
            getPremiumRequestText = "<a href=\"paypal.me/avandyelectronics\">Paypal link</a>";
            getPremiumYesOrNowText = """
                    On the <b>premium</b> tariff:
                    1. News search by keywords is performed <b>every 2 minutes</b>. So you <b>will be the first</b> to receive the <b>most up-to-date information</b>.
                    2. You can add <b>more than 10</b> keywords.
                    Pay for a Premium subscription (<b>50$</b> per year) \uD83D\uDC8E""";
            getPremiumText = "Premium";
            premiumText = " (for a <b>premium account</b>, the search starts <b>every 2 minutes</b>) " + ICON_PREMIUM_IS_ACTIVE;
            initSearchTemplateText = ". %s%s\n[List: <b>%d</b> %s]";
            searchNewsHeaderText = "Search news";
            greetingText = EmojiParser.parseToUnicode("Hello, %s! :blush: \n" +
                    "I can find important information for you and hide a lot of unnecessary information!");
            letsStartText = "Continue?";
            noText = "No";
            yesText = "Yes";
            infoText = "App info and premium";
            settingText = "Settings";
            listKeywordsText = """
                    <b>Keywords</b>
                    - - - - - -
                    For more flexibility you can use the * symbol. Keyword <b>tax**</b> will find all news with <b>tax, taxes, taxi</b> but not <b>taxcom</b>.
                    - - - - - -""";
            listKeywordsButtonText = "Keywords";
            findSelectText = "Search types";
            top20Text = "Frequently used words by period";
            top20Text2 = "<b>Top 20</b> words in ";
            listExcludedText = "Excluding terms";
            deleteUserText = "Delete user data";
            addText = "Add";
            addText2 = "Add keywords";
            delText = "Delete";
            excludeWordText = "Exclude term";
            excludeWordText2 = "Add excluding terms";
            searchText = "Search";
            excludedText = "Excluded";
            undefinedCommandText = "This command doesn't exist or you did not click the button before entering text! " +
                    "However, I will see your message in the journal " + ICON_SMILE_UPSIDE_DOWN;
            changesSavedText = "Done ✔️";
            headlinesNotFound = "» no news headlines found " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» no keywords specified";
            minWordLengthText = "Word length must be more than 2 characters";
            deleteAllWordsText = "❌ all words removed";
            wordIsExistsText = "The word is already on the list: ";
            wordsIsNotAddedText = "No words added";
            wordsAddedText = "Word added";
            aboutDeveloperText = """
                    <b>Developer</b>: <a href="https://github.com/mrprogre">mrprogre</a>
                    <b>E-mail</b>: rps_project@mail.ru
                    <b>Main application:</b> <a href="https://avandy-news.ru/index-en.html">Avandy News Analysis</a> (entry in the Register of Russian Software No: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""";
            yesButtonText = """
                    The first thing to do is add <b>keywords</b>. If you are interested in news related to taxes, for example, then you need to add the word <b>tax**</b> to get all the headlines containing: <b>tax, taxes</b>, etc., but not <b>taxcom</b>. One * is equal to any one word character.
                    - - - - - -
                    Click the "Add" button.""";
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
            sendMessageForDevText = "Write your feedback to the developer";
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
            searchByKeywordsStartText = "» search by keywords";
            fullSearchStartText = "» search all news";
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

    public static String getSettingsText(Settings x, String lang, int isPremium) {
        String premiumPeriod;
        String premiumMessage;

        if (lang.equals("ru")) {
            String schedSettings = "";
            if (x.getScheduler().contains("on") && isPremium != 1) {
                String text = switch (x.getPeriod()) {
                    case "1h" -> " (запуск каждый час)\n";
                    case "2h" -> " (запуск каждые 2 часа)\n";
                    case "24h", "48h", "72h" -> " (запуск один раз в сутки)\n";
                    default ->
                            " (часы запуска: <b>" + getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = "- - - - - -\n" + "<b>5. Старт</b> автопоиска: <b>" + x.getStart() + "</b>" + text;
            }

            if (isPremium == 1) {
                premiumPeriod = "<b>2 min</b>, " + premiumIsActive;
                premiumMessage = "";
            } else {
                premiumPeriod = x.getPeriod();
                premiumMessage = premiumText;
            }

            return "<b>Настройки</b> " + ICON_SETTINGS + " \n" +
                    "<b>1. Автопоиск: " + x.getScheduler() + "</b>\n" +
                    "Автоматический запуск поиска по <b>ключевым словам</b>\n" +
                    "- - - - - -\n" +
                    "<b>2. Частота автопоиска:</b> " + premiumPeriod + "\n" +
                    "Глубина поиска совпадает с частотой и равна текущему времени минус <b>" + x.getPeriod() + "</b>" +
                    premiumMessage + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Исключение</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "Исключение новостей, которые содержат слова-исключения\n" +
                    "- - - - - -\n" +
                    "<b>4. Глубина полного поиска</b>\n" +
                    "Глубина равна текущему времени минус <b>" + x.getPeriodAll() + "</b>\n" +
                    schedSettings +
                    "- - - - - -\n" +
                    "Параметры меняются после нажатия на кнопки";
        } else {
            String schedSettings = "";

            if (x.getScheduler().contains("on") && isPremium != 1) {
                String text = switch (x.getPeriod()) {
                    case "1h" -> " (launch every hour)\n";
                    case "2h" -> " (launch every 2 hours)\n";
                    case "24h", "48h", "72h" -> " (launch once a day)\n";
                    default ->
                            " (start time: <b>" + getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = "- - - - - -\n" + "<b>5. Start</b> auto search by keywords: <b>" + x.getStart() + "</b>" + text;
            }

            if (isPremium == 1) {
                premiumPeriod = "<b>2 min</b>, " + premiumIsActive;
                premiumMessage = "";
            } else {
                premiumPeriod = x.getPeriod();
                premiumMessage = premiumText;
            }

            return "<b>Settings</b> " + ICON_SETTINGS + " \n" +
                    "<b>1. Auto search: " + x.getScheduler() + "</b>\n" +
                    "Automatically launch a search using <b>keywords</b> for the period specified in clause 1, with a frequency in clause 5" + "\n" +
                    "- - - - - -\n" +
                    "<b>2. Auto search frequency: " + premiumPeriod + "</b>\n" +
                    "The search period coincides with the search frequency and is equal to the current time minus <b>" + x.getPeriod() + "</b>" +
                    premiumMessage + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Excluding</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "Excluding news that contains excluding terms\n" +
                    "- - - - - -\n" +
                    "<b>4. Full search interval: " + x.getPeriodAll() + "</b>\n" +
                    schedSettings +
                    "- - - - - -\n" +
                    "Parameters change after pressing buttons";
        }
    }

}