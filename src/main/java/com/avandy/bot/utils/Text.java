package com.avandy.bot.utils;

import com.avandy.bot.model.Settings;
import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static String greetingText, letsStartText, noText, yesText,
            infoText, settingText, listKeywordsText, findSelectText, top20Text, top20Text2, listExcludedText, listRssText,
            startText, deleteUserText, addText, delText, excludeWordText, searchText, excludedText,
            updateTopText, updateTopText2, undefinedCommandText, changeIntervalText, schedulerChangedText, excludedChangedText,
            headlinesNotFound, setupKeywordsText, minWordLengthText, deleteAllWordsText,
            wordIsExistsText, wordsIsNotAddedText, wordsAddedText, startTimeChangedText, aboutDeveloperText,
            yesButtonText, buyButtonText, intervalText, autoSearchText, exclusionText, startSettingsText,
            excludedListText, delFromTopText, searchByTopWordText, top20ByPeriodText, allowCommasAndNumbersText,
            startSearchBeforeText, keywordsSearchText, fullSearchText, chooseSearchStartText,
            addInListText, delFromListText, removeAllText, inputSchedulerStart, sendMessageForDevText,
            actionCanceledText, removeFromTopTenListText, rssSourcesText, fullSearchStartText, chooseNumberWordFromTop,
            searchWithFilterText, searchWithFilter2Text, keywordSearchText, keywordSearch2Text, cancelButtonText,
            foundNewsText, excludedNewsText, settingsNotFoundText, exclusionWordsText, addedExceptionWordsText, wordIsNotInTheListText,
            listText, excludedWordsNotSetText, confirmDeletedUserText, chooseWordDelFromTop,
            chooseSearchDepthText, sendIdeaText, listOfDeletedFromTopText, searchByKeywordsStartText;

    public static void setInterfaceLanguage(String lang) {

        if (lang != null && lang.equals("ru")) {
            greetingText = EmojiParser.parseToUnicode("Здравствуй, %s! :blush: \n" +
                    "Я могу найти для тебя важную информацию и отсеять много лишней!");
            letsStartText = "Продолжим?";
            noText = "Нет";
            yesText = "Да";
            infoText = "Информация";
            settingText = "Настройки";
            listKeywordsText = "Ключевые слова";
            findSelectText = "Выбор вида поиска";
            top20Text = "Top 20 слов за период";
            top20Text2 = "<b>Top 20</b> слов за ";
            listExcludedText = "Слова-исключения";
            listRssText = "Источники RSS";
            startText = "Запуск бота";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            delText = "Удалить";
            excludeWordText = "Исключить слова";
            searchText = "Поиск";
            excludedText = "Исключённое";
            updateTopText = "Обновить топ";
            updateTopText2 = "Tоп 20";
            undefinedCommandText = "Данная команда не существует";
            changeIntervalText = "Интервал изменён ✔️";
            schedulerChangedText = "Режим автопоиска переключён ✔️";
            excludedChangedText = "Режим исключения заголовков переключён ✔️";
            startTimeChangedText = "Время старта автопоиска установлено ✔️";
            headlinesNotFound = EmojiParser.parseToUnicode("» новости не найдены \uD83D\uDCA4");
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
                    Первое, что нужно сделать, - это добавить <b>ключевые слова</b>.
                    Если интересуют новости, связанные с ипотекой, к примеру, то надо добавить слово <b>ипотек</b>, чтобы получить все заголовки, содержащие: <b>ипотекА, ипотекУ</b> и т.д.
                    - - - - - -
                    Второе - добавить через меню <b>слова-исключения</b> для полного поиска, чтобы не читать новости типа: "Как правильно красить ресницы". Просто добавь <b>ресниц</b> в исключения и таких заголовков больше не увидишь!
                    - - - - - -
                    Добавлять можно как одно слово, так и несколько через запятую.
                    - - - - - -
                    На данном этапе мы добавим <b>ключевые слова</b>. Нажми кнопку "Добавить".""";
            buyButtonText = EmojiParser.parseToUnicode("Пока, друг! \uD83D\uDC4B");
            intervalText = "Интервал";
            autoSearchText = "Автопоиск";
            exclusionText = "Исключения";
            startSettingsText = "Старт";
            excludedListText = "Удалённое";
            delFromTopText = "Удалить из топа";
            searchByTopWordText = "Поиск по слову";
            top20ByPeriodText = "<b>Top 20 слов</b> [за ";
            allowCommasAndNumbersText = "Допустимы только цифры или запятые";
            startSearchBeforeText = "Сначала необходимо запустить поиск слов /top";
            keywordsSearchText = "По словам";
            fullSearchText = "Полный поиск";
            addInListText = "Укажите слова для добавления в список (разделять запятой)";
            delFromListText = "Укажите номера слов для удаления (разделять запятой)";
            removeAllText = "удалить всё";
            inputSchedulerStart = "Введите время старта автопоиска (число 0-23)";
            sendMessageForDevText = "Напишите свои предложения разработчику";
            actionCanceledText = "Действие отменено";
            removeFromTopTenListText = "Введите слова для удаления (разделять запятой)";
            rssSourcesText = "Источники новостей";
            searchWithFilterText = "<b>Полный поиск</b> за ";
            searchWithFilter2Text = "исключений";
            keywordSearchText = "Поиск по <b>ключевым словам</b> за ";
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
            chooseSearchDepthText = "Выберите глубину поиска в часах";
            sendIdeaText = "Предложить идею";
            listOfDeletedFromTopText = "Список удалённого из Top 20";
            searchByKeywordsStartText = "» поиск по ключевым словам";
            fullSearchStartText = "» поиск всех новостей";
            chooseNumberWordFromTop = "Нажмите для поиска";
            chooseWordDelFromTop = "Нажмите для удаления";
            chooseSearchStartText = "Выберите час дял расчёта времени запуска автопоиска по ключевым словам";
        } else {
            greetingText = EmojiParser.parseToUnicode("Hello, %s! :blush: \n" +
                    "I can find important information for you and hide a lot of unnecessary information!");
            letsStartText = "Continue?";
            noText = "No";
            yesText = "Yes";
            infoText = "Information";
            settingText = "Settings";
            listKeywordsText = "Keywords";
            findSelectText = "Selecting a search type";
            top20Text = "Top 20 words for the period";
            top20Text2 = "<b>Top 20</b> words in ";
            listExcludedText = "Excluding terms";
            listRssText = "News sources";
            startText = "Bot start";
            deleteUserText = "Delete user data";
            addText = "Add";
            delText = "Delete";
            excludeWordText = "Exclude term";
            searchText = "Search";
            excludedText = "Excluded";
            undefinedCommandText = "Command not defined";
            changeIntervalText = "Interval changed ✔️";
            schedulerChangedText = "Auto search mode switched ✔️";
            excludedChangedText = "Header exclusion mode switched ✔️";
            startTimeChangedText = "Auto search start time is set ✔️";
            headlinesNotFound = EmojiParser.parseToUnicode("» no news headlines found \uD83D\uDCA4");
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
                    The first thing to do is add <b>keywords</b>.
                    If you are interested in news related to taxes, for example, then you need to add the word <b>tax</b> to get all the headlines containing: <b>tax, taxes</b>, etc.
                    - - - - - -
                    The second is to add <b>exception words</b> through the menu for a complete search, so as not to read news like: “How to dye eyelashes correctly.” Just add <b>eyelashes</b> to the exceptions and you won’t see such headlines anymore!
                    - - - - - -
                    You can add one word or several separated by commas.
                    - - - - - -
                    At this point we will add <b>keywords</b>. Click the "Add" button.""";
            buyButtonText = EmojiParser.parseToUnicode("Good buy! \uD83D\uDC4B");
            intervalText = "Interval";
            autoSearchText = "Auto search";
            exclusionText = "News exclusion";
            startSettingsText = "Start";
            excludedListText = "List of deleted";
            delFromTopText = "Delete";
            searchByTopWordText = "Search by word";
            updateTopText = "Update top";
            updateTopText2 = "Show top";
            top20ByPeriodText = "<b>Top 20 words</b> [in ";
            allowCommasAndNumbersText = "Only numbers or commas are allowed";
            startSearchBeforeText = "First you need to run a word search /top";
            keywordsSearchText = "Keywords";
            fullSearchText = "Full search";
            addInListText = "Input words to add (separated by comma)";
            delFromListText = "Input word numbers to remove (separated by comma)";
            removeAllText = "delete all";
            inputSchedulerStart = "Enter the auto search start time (number 0-23)";
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
            chooseSearchDepthText = "Select search time in hours";
            sendIdeaText = "Send feedback";
            listOfDeletedFromTopText = "List of deleted from top";
            searchByKeywordsStartText = "» search by keywords";
            fullSearchStartText = "» search all news";
            chooseNumberWordFromTop = "Click to search";
            chooseWordDelFromTop = "Click to delete";
            chooseSearchStartText = "Select an hour to calculate the time to start auto search by keywords";
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
                schedSettings = "- - - - - -\n" + "<b>5. Старт</b> автопоиска: <b>" + x.getStart() + "</b>" + text;
            }

            return "<b>Настройки</b>\n" +
                    "<b>1. Интервал</b>: <b>-" + x.getPeriod() + "</b>\n" +
                    "Глубина поиска новостей по <b>ключевым словам</b> (интервал поиска равен текущему моменту минус глубина в часах)" + "\n" +
                    "- - - - - -\n" +
                    "<b>2. Интервал</b>: <b>-" + x.getPeriodAll() + "</b>\n" +
                    "Глубина поиска <b>всех новостей</b>" + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Автопоиск: " + x.getScheduler() + "</b>\n" +
                    "Автоматический запуск поиска по <b>ключевым словам</b> за период, указанный в п.1, с частотой в п.5" + "\n" +
                    "- - - - - -\n" +
                    "<b>4. Исключение</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "<b>on</b> - исключение новостей, которые содержат слова-исключения\n" +
                    "<b>off</b> - показывать все новости без исключения\n" +
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
                schedSettings = "- - - - - -\n" + "<b>5. Start</b> auto search by keywords: <b>" + x.getStart() + "</b>" + text;
            }

            return "<b>Settings</b>\n" +
                    "<b>1. Interval</b>: <b>-" + x.getPeriod() + "</b>\n" +
                    "Search time of news search by <b>keywords</b> (search interval is equal to the current moment minus hours)" + "\n" +
                    "- - - - - -\n" +
                    "<b>2. Interval</b>: <b>-" + x.getPeriodAll() + "</b>\n" +
                    "Search time for <b>all news</b>" + "\n" +
                    "- - - - - -\n" +
                    "<b>3. Auto search: " + x.getScheduler() + "</b>\n" +
                    "Automatically launch a search using <b>keywords</b> for the period specified in clause 1, with a frequency in clause 5" + "\n" +
                    "- - - - - -\n" +
                    "<b>4. Excluding</b>: <b>" + x.getExcluded() + "</b>\n" +
                    "<b>on</b> - excluding news that contains exception words\n" +
                    "<b>off</b> - show all news without filter\n" +
                    schedSettings +
                    "- - - - - -\n" +
                    "Parameters change after pressing buttons";
        }
    }


}
