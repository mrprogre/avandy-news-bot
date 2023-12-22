package com.avandy.bot.utils;

import com.avandy.bot.model.Settings;
import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static String greetingText, letsStartText, noText, yesText,
            infoText, settingText, listKeywordsText, findSelectText, top20Text, listExcludedText, listRssText, listTodoText,
            startText, deleteUserText, addText, delText, findAgainText, excludeWordText, findText, excludedText,
            updateTopText, undefinedCommandText, changeIntervalText, schedulerChangedText, excludedChangedText,
            headlinesNotFound, setupKeywordsText, minWordLengthText, deleteAllKeywordsText, deleteAllExcludedText,
            wordIsExistsText, wordsIsNotAddedText, wordsAddedText, startTimeChangedText, aboutDeveloperText;

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
            top20Text = "Топ 20 слов за период";
            listExcludedText = "Слова исключения";
            listRssText = "Источники RSS";
            listTodoText = "Список задач";
            startText = "Запуск бота";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            delText = "Удалить";
            findAgainText = "Искать снова";
            excludeWordText = "Исключить слова";
            findText = "Поиск";
            excludedText = "Исключённое";
            updateTopText = "Обновить топ";
            undefinedCommandText = "Данная команда не существует";
            changeIntervalText = EmojiParser.parseToUnicode("Интервал поиска изменён ✔️");
            schedulerChangedText = EmojiParser.parseToUnicode("Режим автопоиска переключён ✔️");
            excludedChangedText = EmojiParser.parseToUnicode("Режим исключения заголовков переключён ✔️");
            startTimeChangedText = "Время старта автопоиска установлено ✔️";
            headlinesNotFound = EmojiParser.parseToUnicode("» новости не найдены \uD83D\uDCA4");
            setupKeywordsText = "» ключевые слова не заданы";
            minWordLengthText = "Длина слова должна быть более 2 символов";
            deleteAllKeywordsText = "Удалены все ключевые слова ❌";
            deleteAllExcludedText = "Удалены все слова-исключения ❌";
            wordIsExistsText = "Слово уже есть в списке: ";
            wordsIsNotAddedText = "Слова не добавлены";
            wordsAddedText = "Добавлено слов - ";
            aboutDeveloperText = """
            <b>Разработчик</b>: <a href="https://github.com/mrprogre">mrprogre</a>
            <b>Почта</b>: rps_project@mail.ru
            <b>Основная программа:</b> <a href="https://avandy-news.ru">Avandy News Analysis</a> (запись в Реестре российского ПО: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""";
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
            listExcludedText = "Excluded words";
            listRssText = "RSS sources";
            listTodoText = "Task manager";
            startText = "Bot start";
            deleteUserText = "Delete user data";
            addText = "Add";
            delText = "Delete";
            findAgainText = "Find again";
            excludeWordText = "Exclude word";
            findText = "Search";
            excludedText = "Excluded";
            updateTopText = "Update top";
            undefinedCommandText = "Command does not exist";
            changeIntervalText = EmojiParser.parseToUnicode("Search interval changed ✔️");
            schedulerChangedText = EmojiParser.parseToUnicode("Auto search mode switched ✔️");
            excludedChangedText = EmojiParser.parseToUnicode("Header exclusion mode switched ✔️");
            startTimeChangedText = EmojiParser.parseToUnicode("Auto search start time is set ✔️");
            headlinesNotFound = EmojiParser.parseToUnicode("» no news headlines found \uD83D\uDCA4");
            setupKeywordsText = "» no keywords specified";
            minWordLengthText = "Word length must be more than 2 characters";
            deleteAllKeywordsText = "All keywords removed ❌";
            deleteAllExcludedText = "All exception words removed ❌";
            wordIsExistsText = "The word is already on the list: ";
            wordsIsNotAddedText = "No words added";
            wordsAddedText = "Added words - ";
            aboutDeveloperText = """
            <b>Developer</b>: <a href="https://github.com/mrprogre">mrprogre</a>
            <b>E-mail</b>: rps_project@mail.ru
            <b>Main application:</b> <a href="https://avandy-news.ru/index-en.html">Avandy News Analysis</a> (entry in the Register of Russian Software No: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""";

        }
    }

    public static final String YES_BUTTON_TEXT = """
            Первое, что нужно сделать, - это добавить <b>ключевые слова</b>.
            Если интересуют новости, связанные с ипотекой, к примеру, то надо добавить слово <b>ипотек</b>, чтобы получить все заголовки, содержащие: <b>ипотекА, ипотекУ</b> и т.д.
            - - - - - -
            Второе - добавить через меню <b>слова-исключения</b> для полного поиска, чтобы не читать новости типа: "Как правильно красить ресницы". Просто добавь <b>ресниц</b> в исключения и таких заголовков больше не увидишь!
            - - - - - -
            Добавлять можно как одно слово, так и несколько через запятую.
            - - - - - -
            На данном этапе мы добавим <b>ключевые слова</b>. Нажми кнопку "Добавить".""";

    public static final String NO_BUTTON_TEXT = EmojiParser.parseToUnicode("Good buy! \uD83D\uDC4B");

    public static String getSettingsText(Settings x, String scheduler) {
        return "<b>Настройки</b>\n" +
                "<b>1. Интервал</b>: <b>-" + x.getPeriod() + "</b>\n" +
                "Глубина поиска новостей по <b>ключевым словам</b> (интервал поиска равен текущему моменту минус глубина в часах)" + "\n" +
                "- - - - - -\n" +
                "<b>2. Интервал</b>: <b>-" + x.getPeriodAll() + "</b>\n" +
                "Глубина поиска <b>всех новостей</b> и слов для <b>Топ 20</b>" + "\n" +
                "- - - - - -\n" +
                "<b>3. Автопоиск: " + x.getScheduler() + "</b>\n" +
                "Автоматический запуск поиска по <b>ключевым словам</b> за период, указанный в п.1, с частотой в п.5" + "\n" +
                "- - - - - -\n" +
                "<b>4. Исключение</b>: <b>" + x.getExcluded() + "</b>\n" +
                "<b>on</b> - исключение новостей, которые содержат слова-исключения\n" +
                "<b>off</b> - показывать все новости без исключения\n" +
                scheduler +
                "- - - - - -\n" +
                "Параметры меняются после нажатия на кнопки";
    }
}
