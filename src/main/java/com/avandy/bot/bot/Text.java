package com.avandy.bot.bot;

import com.avandy.bot.model.Settings;
import com.avandy.bot.utils.Common;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Text extends Common {
    private static final String delimiterNews = "- - - - - - - - - - - - - - - - - - - - - - - -";
    public static String
            greetingText, letsStartText, noText, yesText, infoText, settingText, listKeywordsText, findSelectText,
            top20Text, top20Text2, listExcludedText, deleteUserText, addText, delText, excludeWordText, searchText,
            excludedText, listKeywordsButtonText, updateTopText, updateTopText2, undefinedCommandText, changesSavedText,
            jaroWinklerSwitcherText, headlinesNotFound, setupKeywordsText, minWordLengthText, deleteAllWordsText,
            jaroWinklerText, wordIsExistsText, wordsIsNotAddedText, wordsAddedText, aboutDeveloperText, addText2,
            yesButtonText, buyButtonText, intervalText, autoSearchText, exclusionText,
            startSettingsText, excludedListText, delFromTopText, top20ByPeriodText, allowCommasAndNumbersText,
            premiumText, startSearchBeforeText, keywordsSearchText, fullSearchText, chooseSearchStartText,
            getPremiumText, addInListText, delFromListText, removeAllText, sendMessageForDevText, searchNewsHeaderText,
            actionCanceledText, removeFromTopTenListText, rssSourcesText, fullSearchStartText, chooseNumberWordFromTop,
            searchWithFilterText, searchWithFilter2Text, keywordSearchText, keywordSearch2Text, cancelButtonText,
            foundNewsText, excludedNewsText, settingsNotFoundText, exclusionWordsText, addedExceptionWordsText,
            wordIsNotInTheListText, listText, excludedWordsNotSetText, confirmDeletedUserText, chooseWordDelFromTop,
            removedFromTopText, chooseSearchDepthText, sendIdeaText, listOfDeletedFromTopText, searchText2,
            getPremiumYesOrNowText, getPremiumRequestText, premiumIsActive, premiumIsActive2, excludeWordText2,
            premiumIsActive3, premiumIsActive4, inputExceptionText, premiumSearchSettingsText, inputExceptionText2,
            sentText, searchByKeywordsStartText, sendPaymentText, sendMessageForDevText2, cleanFullSearchHistoryText,
            historyClearText, addSourceText, delSourceText, addSourceInfoText, sendIdeaText2, saveText2,
            premiumAddSourcesText, notSupportedRssText, rssExistsText, delSourceInfoText, deleteText, listText3,
            rssNameNotExistsText, messageThemeChooseText, noCommasText, changesSavedText2, changesSavedText3,
            listText2, minWordLengthText2, premiumIsActive5, startText, rssSourcesText2, fullSearchText2,
            messageThemeChooseText2, personalSourcesText, saveText, findNewsText, findNewsText2, intervalText2,
            excludeCategoryText, excludedListText2, intervalText3;

    public static void setInterfaceLanguage(String lang) {
        if (lang != null && lang.equals("ru")) {
            startText = "Начало работы с ботом";
            searchNewsHeaderText = "Поиск новостей";
            greetingText = EmojiParser.parseToUnicode("Здравствуйте! :blush: \n" +
                    "Я могу найти для Вас важную информацию и убрать много лишней!");
            letsStartText = "Продолжить?";
            noText = "Нет";
            yesText = "Да";
            infoText = "Информация и подписка";
            settingText = "Настройки";
            listKeywordsButtonText = "Слова для поиска";
            findSelectText = "Выбрать тип поиска";
            top20Text = "Часто употребляемые слова";
            top20Text2 = "Top 20 слов за ";
            listExcludedText = "Слова-исключения";
            deleteUserText = "Удалить данные пользователя";
            addText = "Добавить";
            addText2 = "Добавить слова";
            delText = "Удалить";
            excludeWordText = "Исключить";
            excludeWordText2 = "Добавить слова-исключения";
            searchText = "Поиск";
            searchText2 = "Искать снова";
            excludedText = "Исключённое";
            updateTopText = "Обновить";
            updateTopText2 = "Top 20";
            undefinedCommandText = "Что сделать? " + ICON_SMILE_UPSIDE_DOWN;
            saveText = "Сохранить";
            saveText2 = "Сохранить для автопоиска";
            findNewsText2 = "Искать новости, содержащие слово";
            changesSavedText = "Сохранено ✔️";
            changesSavedText2 = "Сохранено: %s ✔️";
            changesSavedText3 = "Внешний вид сообщений с новостями изменён на: %s ✔️ \n";
            sentText = "Доставлено ✔️";
            historyClearText = "История просмотра очищена ✔️";
            headlinesNotFound = "» новостей нет " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» слова не заданы";
            minWordLengthText = "Длина слов должна быть более 1 символа и до 64";
            minWordLengthText2 = "Длина слов должна быть более 2 символов и до 32";
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
            intervalText = "Интервал";
            intervalText2 = "Изменить интервал поиска";
            intervalText3 = "Изменить интервал";
            autoSearchText = "Автопоиск";
            exclusionText = "Исключение";
            startSettingsText = "Старт";
            excludedListText = "Удалённое";
            excludedListText2 = "Удалённое из топа ";
            delFromTopText = "Удалить";
            top20ByPeriodText = "<b>Top 20 слов</b> за ";
            allowCommasAndNumbersText = "Укажите порядковый номер слова (разделять запятой)";
            startSearchBeforeText = "Топ необходимо обновить. Нажмите на /top";
            keywordsSearchText = "По словам";
            fullSearchText = "Все новости";
            fullSearchText2 = "Все новости";
            addInListText = "Если слов несколько, то <b>разделите их запятыми</b>";
            delFromListText = "Введите <b>порядковый номер</b> слова, а если их несколько, то <b>разделите запятыми</b>";
            removeAllText = "удалить всё";
            sendMessageForDevText = "Написать сообщение разработчику (можно приложить один скриншот)";
            sendMessageForDevText2 = "Направить скриншот оплаты";
            actionCanceledText = "Действие отменено";
            removeFromTopTenListText = "Введите слова для удаления (разделять запятой)";
            rssSourcesText = "<b>Источники новостей</b> (RSS формат)";
            rssSourcesText2 = "Источники новостей";
            searchWithFilterText = "Полный поиск за ";
            searchWithFilter2Text = "слов-исключений";
            keywordSearchText = "Поиск по словам за ";
            keywordSearch2Text = "(слова и фразы)";
            cancelButtonText = "Отменить";
            foundNewsText = "Найдено";
            excludedNewsText = "исключено";
            settingsNotFoundText = "Настройки не обнаружены";
            excludedWordsNotSetText = "» слова-исключения не заданы";
            exclusionWordsText = "Слова-исключения";
            addedExceptionWordsText = "Добавлено слов-исключений";
            wordIsNotInTheListText = "Слово %s отсутствует в списке";
            listText = "Список";
            listText2 = "Список слов";
            listText3 = "Список слов для поиска";
            confirmDeletedUserText = "Подтверждаете удаление пользователя?";
            chooseSearchDepthText = "Интервал поиска равен текущему моменту минус глубина в часах. Выберите глубину поиска";
            sendIdeaText = "Написать отзыв";
            sendIdeaText2 = "Написать разработчику";
            listOfDeletedFromTopText = "Список удалённого из Top 20";
            fullSearchStartText = "» поиск всех новостей за %s";
            searchByKeywordsStartText = "» поиск по словам за %s";
            chooseNumberWordFromTop = "Нажмите для поиска";
            chooseWordDelFromTop = "Нажмите для удаления";
            chooseSearchStartText = "Выберите час для расчёта времени запуска автопоиска по словам";
            removedFromTopText = "не показывать в Топ";
            jaroWinklerSwitcherText = """
                    Чтобы объединить одинаковые слова для исключения повторений можно включить удаление окончаний.
                    Пример: в Top 20 показаны слова: <b>белгородская [10], белгорода [7], белгороде [4]</b>, при включённом удалении будет показано только одно слово <b>белгород [21]</b> c общей суммой ранее указанных слов.
                    Текущий статус:\s""";
            jaroWinklerText = "Окончания";
            getPremiumText = "Премиум бот";
            getPremiumYesOrNowText = "На тарифе <b>Premium bot</b>\n" +
                    "1. Поиск по словам производится каждые <b>2 минуты</b>. " +
                    "Так Вы <b>первым</b> будете получать <b>самую актуальную информацию</b> (можно отключить и поставить другой интервал рассылки);\n" +
                    "2. Можно добавить <b>более " + MAX_KEYWORDS_COUNT + " слов</b> для поиска по словам (до " +
                    MAX_KEYWORDS_COUNT_PREMIUM + ") и <b>более " + MAX_EXCL_TERMS_COUNT + " слов-исключений</b> для полного поиска;\n" +
                    "3. Удаление/добавление <b>источников</b> новостей " +
                    "(если источник предоставляет новости в формате <b>RSS</b>).\n\n" +
                    "Оплатить <b>" + BOT_PRICE_RUS + "</b> рублей за год? \uD83D\uDC8E";
            getPremiumRequestText = """
                    1. <a href="https://qr.nspk.ru/AS1A0047UDL2QR108KLRR4844KTLRSH6?type=01&bank=100000000284&crc=D835">Ссылка на оплату через СБП</a>.
                    В ближайшие 12 часов после отправки подтверждения платежа Вам будут подключены премиум возможности бота.
                    2. <a href="https://avandy-news.ru/refund.html">Возврат средств</a>
                    Не подошёл наш продукт и прошло <b>не более 3 дней</b> с момента активации премиум возможностей - напишите, и мы <b>вернём</b> средства!""";
            premiumIsActive = "активирован премиум " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive2 = "Premium bot активирован до %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive3 = "Добавление <b>более " + MAX_KEYWORDS_COUNT + " слов</b> возможно в /premium режиме";
            premiumIsActive4 = "Максимальное количество слов - " + MAX_KEYWORDS_COUNT_PREMIUM;
            premiumIsActive5 = "Добавление <b>более " + MAX_EXCL_TERMS_COUNT + " слов-исключений</b> возможно в /premium режиме";
            yesButtonText = """
                    Добавьте <b>слова или фразы</b> по которым я буду искать для Вас новости (максимум 3 для начала). При добавлении помните, что СМИ пишут заголовки в максимально простой форме. Не усложняйте!
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Нажмите кнопку <b>Добавить</b> или посмотрите о чём сейчас пишут СМИ, нажав на <b>Top 20</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    <b>Видеоинструкция</b> на <a href="https://youtu.be/HPAk8GPHes4">youtube</a>""";
            inputExceptionText = "Бот ожидал ввода текста. Для продолжения работы нажмите повторно ";
            inputExceptionText2 = "Бот ожидал ввода текста. Для продолжения работы повторите команду";
            premiumSearchSettingsText = "Премиум поиск";
            sendPaymentText = "Отправить скриншот с оплатой";
            cleanFullSearchHistoryText = "Очистить историю ";
            addSourceText = "Добавить RSS";
            delSourceText = "Удалить";
            addSourceInfoText = "Введите <b>страну</b> источника, его <b>название</b> и <b>веб ссылку на XML</b> " +
                    "c новостями, <b>через запятую</b>.\n" +
                    "<b>Примерный</b> список подходящих источников по категориям и странам: " +
                    "<a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            delSourceInfoText = "Удалить <b>персональный</b> источник по его названию";
            premiumAddSourcesText = "Добавление/удаление <b>персональных</b> источников в формате RSS возможно в /premium тарифе\n" +
                    "<b>Примерный</b> список подходящих источников по категориям и странам: <a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            notSupportedRssText = "Данный источник не поддерживается";
            rssExistsText = "Данный источник уже есть в списке";
            rssNameNotExistsText = "Источника с таким названием не существует";
            messageThemeChooseText = "Выберите вариант внешнего вида сообщений. Текущая тема: %s";
            messageThemeChooseText2 = "Внешний вид новостей ";
            noCommasText = "Страну, название и ссылку необходимо разделить запятыми";
            personalSourcesText = "Персональные источники";
            findNewsText = "Искать новости";
            listKeywordsText = """
                    <b>Слова для поиска</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    %s
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Автопоиск %s
                    Запуск поиска раз в %s
                    Внешний вид новостей: %s
                    - - - - - - - - - - - - - - - - - - - - - - - -""";
            excludeCategoryText = "Исключить новости с тематикой";
        } else {
            excludeCategoryText = "Exclude news category";
            listKeywordsText = """
                    <b>Keywords</b>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    %s
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Auto search %s
                    The search runs every <b>%s</b>
                    News appearance: %s
                    - - - - - - - - - - - - - - - - - - - - - - - -""";
            findNewsText = "Find news";
            personalSourcesText = "Personal sources";
            noCommasText = "Country, name and link must be separated by commas";
            messageThemeChooseText = "Select a message appearance option. Current theme: %s";
            messageThemeChooseText2 = "News appearance ";
            rssNameNotExistsText = "There is no source with this name";
            rssExistsText = "This source is already in the list";
            notSupportedRssText = "This source is not supported";
            premiumAddSourcesText = "Adding/removing <b>personal</b> sources (RSS format) is possible in /premium mode\n" +
                    "<b>Sample</b> list of suitable sources by category and country: <a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            addSourceInfoText = "Enter the <b>country</b>, <b>source name</b> and <b>xml link</b>, separated all by commas.\n" +
                    "<b>Sample</b> list of suitable sources by category and country: <a href=\"https://github.com/Martinviv/rss-sources/blob/main/README.md\">rss list</a>";
            delSourceInfoText = "Delete <b>your</b> source by its name";
            addSourceText = "Add RSS";
            delSourceText = "Delete";
            cleanFullSearchHistoryText = "Clear browsing history ";
            sendPaymentText = "Send transfer confirmation";
            premiumSearchSettingsText = "Premium search";
            inputExceptionText = "The bot was waiting for text to be entered. To continue, click again ";
            inputExceptionText2 = "The bot was waiting for text input. To continue, repeat the command";
            yesButtonText = """
                    The first thing to do is add <b>keywords or phrases</b> (maximum 3 to start)
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    You can also watch the <b>video</b> on <a href="https://youtu.be/HPAk8GPHes4">youtube</a>
                    - - - - - - - - - - - - - - - - - - - - - - - -
                    Click the <b>Add keywords</b> button or see what the media are writing about now by clicking on <b>Top 20</b>""";
            premiumIsActive5 = "Adding <b>more than " + MAX_EXCL_TERMS_COUNT + " exclusion terms</b> is possible in /premium mode";
            premiumIsActive4 = "Maximum number of keywords - " + MAX_KEYWORDS_COUNT_PREMIUM;
            premiumIsActive3 = "Adding <b>more than " + MAX_KEYWORDS_COUNT + " keywords</b> is possible in /premium mode";
            premiumIsActive2 = "Premium bot activated until %s " + ICON_PREMIUM_IS_ACTIVE;
            premiumIsActive = "premium is active " + ICON_PREMIUM_IS_ACTIVE;
            getPremiumRequestText = "<a href=\"paypal.me/avandyelectronics\">Paypal link</a>";
            getPremiumYesOrNowText = "On the <b>Premium bot</b> tariff:\n" +
                    "1. News search by keywords is performed every <b>2 minutes</b>. " +
                    "So you <b>will be the first</b> to receive the <b>most up-to-date information</b> (can be disabled);\n" +
                    "2. You can add <b>more than " + MAX_KEYWORDS_COUNT + "</b> keywords (up to " +
                    MAX_KEYWORDS_COUNT_PREMIUM + " words) and <b>more than " + MAX_EXCL_TERMS_COUNT + " excluding terms</b> for full search;\n" +
                    "3. Ability to add <b>personal news sources</b> (if the source provides <b>RSS</b> format) " +
                    "and removing default sources.\n\n" +
                    "Pay for a Premium bot subscription <b>" + BOT_PRICE_ENG + "$</b> per year \uD83D\uDC8E";
            getPremiumText = "Premium bot";
            premiumText = " (for a <b>premium account</b>, the search starts <b>every 2 minutes</b> /premium) " +
                    ICON_PREMIUM_IS_ACTIVE;
            searchNewsHeaderText = "Search news";
            greetingText = EmojiParser.parseToUnicode("Hello! :blush: \n" +
                    "I can find important information for you and hide a lot of unnecessary information!");
            letsStartText = "Continue?";
            noText = "No";
            yesText = "Yes";
            infoText = "App info and premium";
            settingText = "Settings";
            listKeywordsButtonText = "Keywords";
            findSelectText = "Search types";
            top20Text = "Frequently used words by period";
            top20Text2 = "Top 20 words in ";
            listExcludedText = "Excluding terms";
            deleteUserText = "Delete user data";
            addText = "Add";
            addText2 = "Add words";
            delText = "Delete";
            excludeWordText = "Exclude term";
            excludeWordText2 = "Add excluding terms";
            searchText = "Search";
            searchText2 = "Search again";
            excludedText = "Excluded";
            undefinedCommandText = "What to do? " + ICON_SMILE_UPSIDE_DOWN;
            saveText = "Save";
            saveText2 = "Save this word";
            findNewsText2 = "Search news with this word";
            changesSavedText = "Done ✔️";
            changesSavedText2 = "Done: %s ✔️";
            changesSavedText3 = "The appearance of news messages has been changed to: %s ✔️!";
            sentText = "Done ✔️";
            historyClearText = "Browsing history cleared ✔️";
            headlinesNotFound = "» no news headlines found " + ICON_END_SEARCH_NOT_FOUND;
            setupKeywordsText = "» no keywords specified";
            minWordLengthText = "The words length must be more than 1 characters and up to 64";
            minWordLengthText2 = "The words length must be more than 2 characters and up to 32";
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
                    <b>Video:</b> <a href="https://youtu.be/HPAk8GPHes4">youtube</a>""";
            buyButtonText = "Good buy! " + ICON_GOOD_BYE;
            intervalText = "Interval";
            intervalText2 = "Change search interval";
            intervalText3 = "Change search interval";
            autoSearchText = "Auto search";
            exclusionText = "News exclusion";
            startSettingsText = "Start";
            excludedListText = "List of deleted";
            excludedListText2 = "List of deleted ";
            delFromTopText = "Delete";
            updateTopText = "Update top";
            updateTopText2 = "Show top";
            top20ByPeriodText = "<b>Top 20 words</b> in ";
            allowCommasAndNumbersText = "Only numbers or commas are allowed";
            startSearchBeforeText = "First press /top";
            keywordsSearchText = "Keywords";
            fullSearchText = "Full search";
            fullSearchText2 = "All news";
            addInListText = "Input words to add (separated by comma) and send the message";
            delFromListText = "Input word numbers to remove (separated by comma)";
            removeAllText = "delete all";
            sendMessageForDevText = "Write a message to the developer (you can attach one screenshot)";
            sendMessageForDevText2 = "Send a screenshot of payment";
            actionCanceledText = "Cancelled";
            removeFromTopTenListText = "Enter words to delete (separated by comma)";
            rssSourcesText = "<b>News sources</b> (RSS format)";
            rssSourcesText2 = "News sources";
            searchWithFilterText = "Full search in ";
            searchWithFilter2Text = "excluding terms";
            keywordSearchText = "Search by keywords in ";
            keywordSearch2Text = "(words and phrases)";
            cancelButtonText = "Cancel";
            foundNewsText = "Found";
            excludedNewsText = "excluded";
            settingsNotFoundText = "Settings not found";
            excludedWordsNotSetText = "» no excluding terms specified";
            exclusionWordsText = "Excluding terms";
            addedExceptionWordsText = "Added excluding terms";
            wordIsNotInTheListText = "The word <b>%s</b> isn't in the list";
            listText = "List";
            listText2 = "Word list";
            listText3 = "List of words to search";
            confirmDeletedUserText = "Do you confirm?";
            chooseSearchDepthText = "Search time = current moment minus time in hours. Select time.";
            sendIdeaText = "Send feedback";
            sendIdeaText2 = "Write to the developer";
            listOfDeletedFromTopText = "List of deleted from the Top";
            fullSearchStartText = "» search all news in %s";
            searchByKeywordsStartText = "» word search in %s";
            chooseNumberWordFromTop = "Click to search";
            chooseWordDelFromTop = "Click to delete";
            chooseSearchStartText = "Select an hour to calculate the time to start auto search by keywords";
            removedFromTopText = "don't show in Top";
            jaroWinklerText = "Word endings";
            jaroWinklerSwitcherText = """
                    To combine identical words to eliminate repetitions, you can enable removal of word endings
                    For example: "played, player" will be combined into one word "play".
                    Current status:\s""";
            startText = "Getting started with the bot";
        }
    }

    public static String getSettingsText(Settings settings, String lang, int isPremium) {
        String premiumSettings = "";
        String schedSettings = "";
        String premiumPeriod, start, start1, start2, start24, startDefault, on1, on2, off1, off2, premium1, premium2,
                settings1, autoSearch1, autoSearch2, autoSearch3, fullSearch1, change1, change2, filter1,
                filter2, theme1, theme2;

        if (lang.equals("ru")) {
            start = "<b>Старт</b> автопоиска: <b>" + settings.getStart() + "</b>\n" +
                    "[изменить /change_time]\n" +
                    "<pre>%s</pre>";
            start1 = "Запуск поиска каждый час\n";
            start2 = "Запуск поиска каждые 2 часа\n";
            start24 = "Запуск поиска один раз в сутки\n";
            startDefault = "Часы запуска: <b>%s:00</b>\n";
            on1 = "включён";
            on2 = "включить";
            off1 = "выключен";
            off2 = "выключить";
            premium1 = "Премиум поиск";
            premium2 = "Поиск производится каждые 2 минуты";
            settings1 = "Настройки";
            autoSearch1 = "<b>Автопоиск</b> по словам";
            autoSearch2 = "<b>Частота и интервал</b> автопоиска";
            autoSearch3 = "В результатах поиска представлены новости за выбранный интервал";
            change1 = "[изменить /interval]";
            change2 = "изменить";
            fullSearch1 = "<b>Полный поиск</b> новостей, интервал";
            filter1 = "Фильтрация новостей";
            filter2 = "Новости, содержащие слова-исключения, не будут показаны";
            theme1 = "<b>Внешний вид</b> ленты новостей, тип";
            theme2 = "выбрать другой тип";
        } else {
            start = "<b>Start</b> auto search: <b>" + settings.getStart() + "</b> [/change_time]\n" +
                    "<pre>%s</pre>";
            start1 = "Launch every hour\n";
            start2 = "Launch every 2 hours\n";
            start24 = "Launch  once a day\n";
            startDefault = "Start time: <b>%s:00</b>\n";
            on1 = "on";
            on2 = "turn";
            off1 = "off";
            off2 = "turn";
            premium1 = "Premium bot search";
            premium2 = "Enable or disable auto search every 2 minutes (will be launched in accordance with step 2)";
            settings1 = "Settings";
            autoSearch1 = "<b>Auto search</b> by words";
            autoSearch2 = "<b>Frequency and interval</b> auto search";
            autoSearch3 = "The search results show news for the selected interval";
            change1 = "[change /interval]";
            change2 = "change";
            fullSearch1 = "<b>Full search</b> news, interval";
            filter1 = "News filtering";
            filter2 = "News containing exception words will not be shown";
            theme1 = "<b>Appearance</b> news feed, type";
            theme2 = "choose another type";
        }

        if (settings.getScheduler().contains(on1) && isPremium != 1) {
            String text = switch (settings.getPeriod()) {
                case "1h" -> start1;
                case "2h" -> start2;
                case "24h", "48h", "72h" -> start24;
                default -> String.format(startDefault, getTimeToExecute(settings.getStart(), settings.getPeriod()));
            };
            schedSettings = String.format(start, text);
        }

        String premiumRus = settings.getPremiumSearch();
        String onOffPremiumRus = on2 + " /prem_on";
        if (premiumRus.contains("on") || premiumRus.contains(on1)) {
            onOffPremiumRus = off2 + " /prem_off";
            if (isPremium == 1) {
                change1 = "";
            }
        }

        if (isPremium == 1) {
            String period = settings.getPremiumSearch().equals("on") ? "2 min" : settings.getPeriod();
            premiumPeriod = "<b>" + period + "</b>";
            String rus = settings.getPremiumSearch().equals("on") ? "<b>" + on1 + "</b>" : "<b>" + off1 + "</b>";
            premiumSettings = delimiterNews + "\n<b>" + premium1 + ":</b> " + rus + "\n" +
                    "[" + onOffPremiumRus + "]\n" +
                    "<pre>" + premium2 + "</pre>";
        } else {
            premiumPeriod = settings.getPeriod();
        }

        String schedulerRus = settings.getScheduler();
        String onOffSchedulerRus = on2 + " /on";
        if (schedulerRus.contains("on") || schedulerRus.contains(on1)) onOffSchedulerRus = off2 + " /off";

        String excludedRus = settings.getExcluded();
        String onOffExcludedRus = on2 + " /filter_on";
        if (excludedRus.contains("on") || excludedRus.contains(on1))
            onOffExcludedRus = off2 + " /filter_off";

        String exclRus = settings.getExcluded();
        if (lang.equals("ru")) {
            schedulerRus = minusOnOff(schedulerRus);
            exclRus = minusOnOff(exclRus);
        }

        return String.format("""
                        <b>%s</b> %s\s
                        %s
                        %s: <b>%s</b> [%s]
                        %s: <b>%s</b> %s
                        <pre>%s</pre>
                        %s%s%s
                        %s: <b>%s</b>\s
                        [%s /interval_full]
                        <b>%s</b>: <b>%s</b> [%s]
                        <pre>%s</pre>%s
                        %s: <b>%s</b>\s
                        %s[ /theme]""",
                settings1, ICON_SETTINGS,
                delimiterNews,
                autoSearch1, schedulerRus, onOffSchedulerRus,
                autoSearch2, premiumPeriod, change1,
                autoSearch3,
                schedSettings, premiumSettings, delimiterNews,
                fullSearch1, settings.getPeriodAll(),
                change2,
                filter1, exclRus, onOffExcludedRus,
                filter2, delimiterNews,
                theme1, settings.getMessageTheme(),
                theme2);
    }

}