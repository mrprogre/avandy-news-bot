package com.avandy.bot.utils;

import com.avandy.bot.model.Settings;
import com.avandy.bot.service.TelegramBot;
import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static String greetingText, letsStartText, noText, yesText,
            infoText, settingText, listKeywordsText, findSelectText, top20Text, top20Text2, listExcludedText, listRssText,
            deleteUserText, addText, delText, excludeWordText, searchText, excludedText,
            updateTopText, updateTopText2, undefinedCommandText, changesSavedText, jaroWinklerSwitcherText,
            headlinesNotFound, setupKeywordsText, minWordLengthText, deleteAllWordsText, jaroWinklerText,
            wordIsExistsText, wordsIsNotAddedText, wordsAddedText, aboutDeveloperText, initSearchTemplateText,
            yesButtonText, buyButtonText, intervalText, autoSearchText, exclusionText, startSettingsText,
            excludedListText, delFromTopText, top20ByPeriodText, allowCommasAndNumbersText,
            startSearchBeforeText, keywordsSearchText, fullSearchText, chooseSearchStartText,
            addInListText, delFromListText, removeAllText, sendMessageForDevText, searchNewsHeaderText,
            actionCanceledText, removeFromTopTenListText, rssSourcesText, fullSearchStartText, chooseNumberWordFromTop,
            searchWithFilterText, searchWithFilter2Text, keywordSearchText, keywordSearch2Text, cancelButtonText,
            foundNewsText, excludedNewsText, settingsNotFoundText, exclusionWordsText, addedExceptionWordsText, wordIsNotInTheListText,
            listText, excludedWordsNotSetText, confirmDeletedUserText, chooseWordDelFromTop, removedFromTopText,
            chooseSearchDepthText, sendIdeaText, listOfDeletedFromTopText, searchByKeywordsStartText;

    public static void setInterfaceLanguage(String lang) {
        if (lang != null && lang.equals("ru")) {
            initSearchTemplateText = ". %s%s\n[Список: <b>%d</b> %s]";
            searchNewsHeaderText = "Поиск новостей";
            greetingText = EmojiParser.parseToUnicode("Здравствуй, %s! :blush: \n" +
                    "Я могу найти для тебя важную информацию и отсеять много лишней!");
            letsStartText = "Продолжим?";
            noText = "Нет";
            yesText = "Да";
            infoText = "Информация о приложении";
            settingText = "Настройки";
            listKeywordsText = "Ключевые слова";
            findSelectText = "Выбрать тип поиска";
            top20Text = "Часто употребляемые слова";
            top20Text2 = "<b>Top 20</b> слов за ";
            listExcludedText = "Слова-исключения";
            listRssText = "Источники RSS";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            delText = "Удалить";
            excludeWordText = "Исключить";
            searchText = "Поиск";
            excludedText = "Исключённое";
            updateTopText = "Обновить топ";
            updateTopText2 = "Tоп 20";
            undefinedCommandText = "Данная команда не существует или Вы не нажали кнопку Добавить перед вводом";
            changesSavedText = "Сохранено ✔️";
            headlinesNotFound = "» новости не найдены " + TelegramBot.ICON_END_SEARCH_NOT_FOUND;
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
                    Первое, что нужно сделать, - добавить <b>ключевые слова</b>. Если интересуют новости, связанные с ипотекой, к примеру, то добавь слово <b>ипотек</b>, чтобы получить все заголовки, содержащие: <b>ипотекА, ипотекУ</b> и т.д.
                    - - - - - -
                    Нажми кнопку "Добавить".""";
            buyButtonText = "Пока, друг! " + TelegramBot.ICON_GOOD_BYE;
            intervalText = "Интервал";
            autoSearchText = "Автопоиск";
            exclusionText = "Исключения";
            startSettingsText = "Старт";
            excludedListText = "Удалённое";
            delFromTopText = "Удалить из топа";
            top20ByPeriodText = "<b>Top 20 слов</b> за ";
            allowCommasAndNumbersText = "Допустимы только цифры или запятые";
            startSearchBeforeText = "Топ необходимо обновить. Нажмите на /top";
            keywordsSearchText = "По словам";
            fullSearchText = "Полный";
            addInListText = "Введите слова для добавления в список (разделять запятой) и нажмите \"Отправить\"";
            delFromListText = "Укажите номера слов для удаления (разделять запятой)";
            removeAllText = "удалить всё";
            sendMessageForDevText = "Напишите свои предложения разработчику";
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
            sendIdeaText = "Предложить идею";
            listOfDeletedFromTopText = "Список удалённого из Top 20";
            searchByKeywordsStartText = "» поиск по ключевым словам";
            fullSearchStartText = "» поиск всех новостей";
            chooseNumberWordFromTop = "Нажмите для поиска";
            chooseWordDelFromTop = "Нажмите для удаления";
            chooseSearchStartText = "Выберите час дял расчёта времени запуска автопоиска по ключевым словам";
            removedFromTopText = "удалено из Топа";
            jaroWinklerSwitcherText = """
                    Проверка схожести слов методом <b>Джаро-Винклера</b>. Когда проверка включена - однокоренные слова с разными окончаниями объединяются в одно общее слово,чтобы не забивать Топ одним и тем же.
                    Пример: если в Топе есть строки со словами: <b>белгородская [10], белгороду [7], белгороде [4]</b>, то при включённом J-W будет одна строка  <b>белгород [21]</b> c общей суммой упоминаний вышеуказанных слов.
                    Текущий статус:\s""";
            jaroWinklerText = "Jaro-Winkler";
        } else {
            initSearchTemplateText = ". %s%s\n[List: <b>%d</b> %s]";
            searchNewsHeaderText = "Search news";
            greetingText = EmojiParser.parseToUnicode("Hello, %s! :blush: \n" +
                    "I can find important information for you and hide a lot of unnecessary information!");
            letsStartText = "Continue?";
            noText = "No";
            yesText = "Yes";
            infoText = "Application information";
            settingText = "Settings";
            listKeywordsText = "Keywords";
            findSelectText = "Search types";
            top20Text = "Frequently used words by period";
            top20Text2 = "<b>Top 20</b> words in ";
            listExcludedText = "Excluding terms";
            listRssText = "News sources";
            deleteUserText = "Delete user data";
            addText = "Add";
            delText = "Delete";
            excludeWordText = "Exclude term";
            searchText = "Search";
            excludedText = "Excluded";
            undefinedCommandText = "Command not defined";
            changesSavedText = "Done ✔️";
            headlinesNotFound = "» no news headlines found " + TelegramBot.ICON_END_SEARCH_NOT_FOUND;
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
                    The first thing to do is add <b>keywords</b>. If you are interested in news related to taxes, for example, then you need to add the word <b>tax</b> to get all the headlines containing: <b>tax, taxpayer, taxes</b>, etc.
                    - - - - - -
                    Click the "Add" button.""";
            buyButtonText = "Good buy! " + TelegramBot.ICON_GOOD_BYE;
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
            jaroWinklerText = "Jaro-Winkler";
            jaroWinklerSwitcherText = "<b>on/off</b> check for string similarity using the <b>Jaro-Winkler</b> method " +
                    "(default similarity level = <b>85%</b>, therefore the words: \"played, player\" will be combined into " +
                    "the word \"play\", because their similarity to each other is 88%).\n" +
                    "Current status: ";
        }
    }

    public static String getSettingsText(Settings x, String lang) {
        if (lang.equals("ru")) {
            String schedSettings = "";
            if (x.getScheduler().equals("on")) {
                String text = switch (x.getPeriod()) {
                    case "1h" -> " (запуск каждый час)\n";
                    case "2h" -> " (запуск каждые 2 часа)\n";
                    case "24h", "48h", "72h" -> " (запуск один раз в сутки)\n";
                    default ->
                            " (часы запуска: <b>" + Common.getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = "- - - - - -\n" + "<b>4. Старт</b> автопоиска: <b>" + x.getStart() + "</b>" + text;
            }

            return "<b>Настройки</b> " + TelegramBot.ICON_SETTINGS + " \n" +
                    "<b>1. Исключение</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "<b>on</b> - исключение новостей, которые содержат слова-исключения\n" +
                    "<b>off</b> - показывать все новости без исключения\n" +
                    "- - - - - -\n" +
                    "<b>2. Автопоиск: " + x.getScheduler() + "</b>\n" +
                    "Автоматический запуск поиска по <b>ключевым словам</b> за период" + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Интервал автопоиска: " + x.getPeriod() + "</b>\n" +
                    "Интервал равен текущему времени минус глубина поиска в часах" + "\n" +
                    schedSettings +
                    "- - - - - -\n" +
                    "Параметры меняются после нажатия на кнопки";
        } else {
            String schedSettings = "";
            if (x.getScheduler().equals("on")) {
                String text = switch (x.getPeriod()) {
                    case "1h" -> " (launch every hour)\n";
                    case "2h" -> " (launch every 2 hours)\n";
                    case "24h", "48h", "72h" -> " (launch once a day)\n";
                    default ->
                            " (start time: <b>" + Common.getTimeToExecute(x.getStart(), x.getPeriod()) + ":00</b>)\n";
                };
                schedSettings = "- - - - - -\n" + "<b>4. Start</b> auto search by keywords: <b>" + x.getStart() + "</b>" + text;
            }

            return "<b>Settings</b> " + TelegramBot.ICON_SETTINGS + " \n" +
                    "<b>1. Excluding</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "<b>on</b> - excluding news that contains excluding terms\n" +
                    "<b>off</b> - show all news without filter\n" +
                    "- - - - - -\n" +
                    "<b>2. Auto search: " + x.getScheduler() + "</b>\n" +
                    "Automatically launch a search using <b>keywords</b> for the period specified in clause 1, with a frequency in clause 5" + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Auto search interval: " + x.getPeriod() + "</b>\n" +
                    "The search period is equal to the current moment minus hours" + "\n" +
                    schedSettings +
                    "- - - - - -\n" +
                    "Parameters change after pressing buttons";
        }
    }


}
