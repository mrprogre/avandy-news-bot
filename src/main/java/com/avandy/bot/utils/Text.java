package com.avandy.bot.utils;

import com.avandy.bot.model.Settings;
import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static final String INFO_RUS = "Информация";
    public static final String SETTINGS_RUS = "Настройки";
    public static final String LIST_KEYWORDS_RUS = "Ключевые слова";
    public static final String FIND_RUS = "Выбор вида поиска";
    public static final String TOP_20_RUS = "Топ 20 слов за период";
    public static final String LIST_EXCLUDED_RUS = "Слова исключения";
    public static final String LIST_RSS_RUS = "Источники RSS";
    public static final String LIST_TODO_RUS = "Список задач";
    public static final String START_RUS = "Запуск бота";
    public static final String DELETE_USER_RUS = "Удалить данные пользователя";

    public static final String START_SEARCH = "START_SEARCH";
    public static final String NEXT_ICON = "» » »";
    public static final String LIST_KEYWORDS = "LIST_KEYWORDS";
    public static final String ADD = "ADD";
    public static final String ADD_RUS = "Добавить";
    public static final String DEL_RUS = "Удалить";
    public static final String FIND_AGAIN_RUS = "Искать снова";
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

    public static String getString(String userFirstname) {
        return EmojiParser.parseToUnicode("Здравствуй, " + userFirstname + "! :blush: \n" +
                "Я могу найти для тебя важную информацию и отсеять много лишней!");
    }

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
