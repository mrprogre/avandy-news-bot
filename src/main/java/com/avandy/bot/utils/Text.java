package com.avandy.bot.utils;

import com.vdurmont.emoji.EmojiParser;

public class Text {
    public static final String START_SEARCH = "START_SEARCH";
    public static final String NEXT_ICON = "» » »";
    public static final String LIST_KEYWORDS = "LIST_KEYWORDS";
    public static final String LIST_KEYWORDS_RUS = "Ключевые слова";
    public static final String ADD = "ADD";
    public static final String ADD_RUS = "Добавить";

    public static final String UPDATE_PERIOD_CHANGED = EmojiParser.parseToUnicode("Интервал поиска изменён ✔️");
    public static final String HEADLINES_NOT_FOUND = EmojiParser.parseToUnicode("» новости не найдены \uD83D\uDCA4");
    public static final String SET_UP_KEYWORDS = "» ключевые слова не заданы";

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
}
