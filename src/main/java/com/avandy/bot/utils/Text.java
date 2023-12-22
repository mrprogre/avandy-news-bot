package com.avandy.bot.utils;

import com.avandy.bot.model.Settings;
import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static String greetingText, letsStartText, noText, yesText,
            infoText, settingText, listKeywordsText, findSelectText, top20Text, listExcludedText, listRssText, listTodoText,
            startText, deleteUserText, addText, delText, findAgainText, excludeWordText, findText, excludedText;

    public static void setInterfaceLanguage(String lang) {

        if (lang != null && lang.equals("ru")) {
            greetingText = EmojiParser.parseToUnicode("Здравствуй! :blush: \n" +
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
        } else {
            greetingText = EmojiParser.parseToUnicode("Hello! :blush: \n" +
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
        }
    }

    public static final String UPDATE_PERIOD_CHANGED = EmojiParser.parseToUnicode("Интервал поиска изменён ✔️");
    public static final String SCHEDULER_CHANGED = EmojiParser.parseToUnicode("Режим автопоиска переключён ✔️");
    public static final String EXCLUDED_CHANGED = EmojiParser.parseToUnicode("Режим исключения заголовков переключён ✔️");
    public static final String HEADLINES_NOT_FOUND = EmojiParser.parseToUnicode("» новости не найдены \uD83D\uDCA4");
    public static final String SET_UP_KEYWORDS = "» ключевые слова не заданы";
    public static final String MIN_WORD_LENGTH = "Длина слова должна быть более 2 символов";
    public static final String DELETE_ALL_KEYWORDS = "Удалены все ключевые слова ❌";
    public static final String DELETE_ALL_EXCLUDED = "Удалены все слова-исключения ❌";
    public static final String WORD_IS_EXISTS = "Слово уже есть в списке: ";
    public static final String WORDS_IS_NOT_ADD = "Слова не добавлены";
    public static final String WORDS_ADDED = "Добавлено слов - ";

    public static final String INFO = """
            <b>Разработчик</b>: <a href="https://github.com/mrprogre">mrprogre</a>
            <b>Почта</b>: rps_project@mail.ru
            <b>Основная программа:</b> <a href="https://avandy-news.ru">Avandy News Analysis</a> (запись в Реестре российского ПО: <a href="https://reestr.digital.gov.ru/reestr/1483979/">17539</a>)""";


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
