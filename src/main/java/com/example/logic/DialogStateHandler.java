package com.example.logic;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import com.example.feed.NewsSearch;
import com.example.feed.SearchResult;
import com.example.storage.DialogState;
import com.example.storage.DialogStep;
import com.example.storage.Feed;
import com.example.storage.FeedPosition;
import com.example.storage.Interval;
import com.example.storage.MailingSettings;
import com.example.storage.Storage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик ответов на шаги диалогов бота: добавления ленты, настройки
 * рассылки, показа и удаления лент, размера страницы. Здесь же живут
 * вспомогательные операции с лентами, нужные и командам бота.
 */
class DialogStateHandler {

    /**
     * Максимальный размер страницы новостей.
     */
    static final int MAX_PAGE_SIZE = 20;

    /**
     * Подпись кнопки "Показать ещё".
     */
    static final String MORE_TEXT = "Показать ещё";

    /**
     * Данные кнопки "Показать ещё".
     */
    static final String MORE_DATA = "more";

    /**
     * Префикс данных кнопки следующей страницы поиска.
     */
    static final String SEARCH_MORE_PREFIX = "search-more:";

    /**
     * Ответ, когда лента с такой ссылкой уже есть в списке.
     */
    static final String DUPLICATE_MESSAGE = "Эта лента уже есть в списке";

    /**
     * Ответ на неверную ссылку в диалоге добавления ленты.
     */
    private static final String URL_ERROR_MESSAGE =
            "Не удалось найти рассылку. Пришлите ссылку ещё раз или /cancel";

    /**
     * Ответ на неверный размер страницы.
     */
    static final String PAGE_SIZE_ERROR_MESSAGE =
            "Размер страницы должен быть числом от 1 до " + MAX_PAGE_SIZE;

    /**
     * Ответ на неверный интервал проверки лент.
     */
    private static final String INTERVAL_ERROR_MESSAGE =
            "Интервал должен быть от 1 секунды до 7 дней, например: 30, 15m, 2h, 1d";

    /**
     * Просьба прислать час конца тихих часов.
     */
    private static final String ASK_QUIET_TO_MESSAGE = "До какого часа?";

    /**
     * Ответ на неверный час тихих часов.
     */
    private static final String QUIET_HOUR_ERROR_MESSAGE =
            "Пришлите час от 0 до 23 или /cancel";

    /**
     * Просьба прислать ключевые слова поиска.
     */
    private static final String ASK_SEARCH_QUERY_MESSAGE = "Какие слова искать?";

    /**
     * Ответ, когда ленты с присланным названием нет в списке.
     */
    private static final String SEARCH_FEED_ERROR_MESSAGE =
            "Такой ленты нет в списке. Пришлите название ещё раз или /cancel";

    /**
     * Ответ на пустой запрос поиска.
     */
    private static final String SEARCH_QUERY_ERROR_MESSAGE =
            "Укажите ключевые слова для поиска";

    /**
     * Ответ, когда у пользователя нет подписок.
     */
    private static final String NO_SUBSCRIPTIONS_MESSAGE =
            "Подписок пока нет. Добавь ленту командой /add <url>";

    /**
     * Ответ, когда поиск ничего не нашёл.
     */
    private static final String NO_MATCHES_MESSAGE = "Ничего не найдено";

    /**
     * Ответ, когда ленту не удалось прочитать.
     */
    private static final String SEARCH_READ_ERROR_MESSAGE = "Не удалось прочитать ленту";

    /**
     * Хранилище данных пользователей.
     */
    private final Storage storage;

    /**
     * Чтение новостей из лент.
     */
    private final NewsReader reader;

    /**
     * Поиск новостей по ключевым словам.
     */
    private final NewsSearch search;

    /**
     * Результаты поиска, у которых ещё есть следующая страница.
     */
    private final Map<Long, SearchPage> searchPages = new ConcurrentHashMap<>();

    /**
     * Обработчики ответов по шагам диалога.
     */
    private final Map<DialogStep, StepAnswer> stepAnswers =
            new EnumMap<>(DialogStep.class);

    /**
     * Создаёт обработчик диалогов и регистрирует обработчики шагов.
     */
    DialogStateHandler(Storage storage, NewsReader reader) {
        this.storage = storage;
        this.reader = reader;
        this.search = new NewsSearch(reader);
        stepAnswers.put(DialogStep.WAITING_URL,
                (userId, state, text) -> new Answer(urlAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_NAME,
                (userId, state, text) -> new Answer(nameAnswer(userId, state.url(), text)));
        stepAnswers.put(DialogStep.WAITING_INTERVAL,
                (userId, state, text) -> new Answer(intervalValueAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_QUIET_FROM,
                (userId, state, text) -> new Answer(quietFromAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_QUIET_TO,
                (userId, state, text) -> new Answer(quietToAnswer(userId, state.url(), text)));
        stepAnswers.put(DialogStep.WAITING_LATEST,
                (userId, state, text) -> latestDialogAnswer(userId, text));
        stepAnswers.put(DialogStep.WAITING_REMOVE,
                (userId, state, text) -> new Answer(removeDialogAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_PAGE_SIZE,
                (userId, state, text) -> new Answer(pageSizeDialogAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_SEARCH_FEED,
                (userId, state, text) -> new Answer(searchFeedAnswer(userId, text)));
        stepAnswers.put(DialogStep.WAITING_SEARCH_QUERY,
                (userId, state, text) -> searchQueryAnswer(userId, state.url(), text));
    }

    /**
     * Возвращает ответ на шаг диалога, к которому прислал сообщение
     * пользователь.
     */
    Answer answer(long userId, DialogState state, String text) {
        StepAnswer stepAnswer = stepAnswers.get(state.step());
        if (stepAnswer == null) {
            throw new IllegalStateException("Неожиданный шаг диалога: " + state.step());
        }
        return stepAnswer.handle(userId, state, text);
    }

    /**
     * Проверяет ссылку, присланную в диалоге. Если по ней есть лента, диалог
     * переходит к шагу названия, иначе остаётся на том же шаге.
     */
    private String urlAnswer(long userId, String url) {
        if (!isFeed(url)) {
            return URL_ERROR_MESSAGE;
        }
        if (hasFeed(userId, url)) {
            storage.setDialogState(userId, DialogState.NO_DIALOG);
            return DUPLICATE_MESSAGE;
        }
        storage.setDialogState(userId, new DialogState(DialogStep.WAITING_NAME, url));
        return "Лента найдена. Как её назвать?";
    }

    /**
     * Сохраняет ленту с названием, присланным в диалоге, и завершает диалог.
     * Если название пустое или уже занято другой лентой, диалог остаётся на
     * шаге названия.
     */
    private String nameAnswer(long userId, String url, String name) {
        if (name.isEmpty()) {
            return "Название не может быть пустым. Как назвать ленту?";
        }
        if (hasFeedName(userId, name)) {
            return "Лента с таким названием уже есть. Пришлите другое название или /cancel";
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        if (!storage.add(userId, new Feed(name, url))) {
            return DUPLICATE_MESSAGE;
        }
        return "Лента “" + name + "” добавлена";
    }

    /**
     * Сохраняет интервал проверки лент, присланный параметром команды или в
     * диалоге. Если интервал указан неверно, диалог остаётся на шаге интервала.
     */
    String intervalValueAnswer(long userId, String text) {
        Interval interval = createInterval(text);
        if (interval == null) {
            return INTERVAL_ERROR_MESSAGE;
        }
        MailingSettings settings = storage.getMailingSettings(userId);
        settings.setInterval(interval);
        storage.setMailingSettings(userId, settings);
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return "Буду проверять ленты " + interval.format();
    }

    /**
     * Создаёт интервал по строке вида "30", "30s", "15m", "2h" или "1d".
     * Число без буквы считается минутами. Возвращает null, если строка не
     * похожа на интервал или интервал не от секунды до недели.
     */
    private Interval createInterval(String text) {
        if (!text.matches("\\d+[smhd]?")) {
            return null;
        }
        int unitIndex = text.length();
        long multiplier = 60;
        if (Character.isLetter(text.charAt(unitIndex - 1))) {
            unitIndex--;
            multiplier = switch (text.charAt(unitIndex)) {
                case 's' -> 1;
                case 'm' -> 60;
                case 'h' -> 60 * 60;
                default -> 24 * 60 * 60;
            };
        }
        try {
            return new Interval(Math.toIntExact(
                    Long.parseLong(text.substring(0, unitIndex)) * multiplier));
        } catch (ArithmeticException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Запоминает час начала тихих часов, присланный в диалоге, и переходит
     * к шагу часа конца. Если час указан неверно, диалог остаётся на том же
     * шаге.
     */
    String quietFromAnswer(long userId, String text) {
        Integer from = parseHour(text);
        if (from == null) {
            return QUIET_HOUR_ERROR_MESSAGE;
        }
        storage.setDialogState(userId,
                new DialogState(DialogStep.WAITING_QUIET_TO, from.toString()));
        return ASK_QUIET_TO_MESSAGE;
    }

    /**
     * Сохраняет тихие часы, когда в диалоге прислан час конца, и завершает
     * диалог. Если час указан неверно, диалог остаётся на шаге часа конца.
     */
    String quietToAnswer(long userId, String fromText, String text) {
        Integer to = parseHour(text);
        if (to == null) {
            return QUIET_HOUR_ERROR_MESSAGE;
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return saveQuietHours(userId, Integer.parseInt(fromText), to);
    }

    /**
     * Отключает тихие часы пользователя.
     */
    String disableQuietHours(long userId) {
        MailingSettings settings = storage.getMailingSettings(userId);
        settings.setQuietFromHour(null);
        settings.setQuietToHour(null);
        storage.setMailingSettings(userId, settings);
        return "Тихие часы отключены";
    }

    /**
     * Сохраняет тихие часы пользователя и возвращает ответ пользователю.
     */
    String saveQuietHours(long userId, int from, int to) {
        MailingSettings settings = storage.getMailingSettings(userId);
        settings.setQuietFromHour(from);
        settings.setQuietToHour(to);
        storage.setMailingSettings(userId, settings);
        return "Не буду присылать новости с " + formatHour(from)
                + " до " + formatHour(to);
    }

    /**
     * Удаляет ленту, присланную в диалоге, и завершает диалог. Если такой
     * ленты нет в списке, диалог остаётся на том же шаге.
     */
    private String removeDialogAnswer(long userId, String text) {
        if (!storage.remove(userId, findFeedUrl(userId, text))) {
            return "Такой ленты нет в списке. Пришлите название ещё раз или /cancel";
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return "Лента удалена";
    }

    /**
     * Показывает первую страницу новостей ленты, присланной в диалоге, и
     * завершает диалог. Если ленту не удалось прочитать, диалог остаётся на
     * том же шаге.
     */
    private Answer latestDialogAnswer(long userId, String text) {
        String url = findFeedUrl(userId, text);
        if (!isFeed(url)) {
            return new Answer("Не удалось прочитать ленту. Пришлите название ещё раз или /cancel");
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return showPage(userId, url, 0);
    }

    /**
     * Сохраняет размер страницы, присланный в диалоге, и завершает диалог.
     * Если размер указан неверно, диалог остаётся на том же шаге.
     */
    private String pageSizeDialogAnswer(long userId, String text) {
        if (!changePageSize(userId, text)) {
            return PAGE_SIZE_ERROR_MESSAGE + ". Пришлите число ещё раз или /cancel";
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return "Размер страницы изменён: " + storage.getPageSize(userId);
    }

    /**
     * Запоминает, где искать новости, присланное в диалоге: all или название
     * ленты, и переходит к шагу ключевых слов. Если ленты с таким названием
     * нет в списке, диалог остаётся на том же шаге.
     */
    private String searchFeedAnswer(long userId, String text) {
        if (text.equalsIgnoreCase("all")) {
            storage.setDialogState(userId,
                    new DialogState(DialogStep.WAITING_SEARCH_QUERY, "all"));
            return ASK_SEARCH_QUERY_MESSAGE;
        }
        if (findFeedByName(userId, text) == null) {
            return SEARCH_FEED_ERROR_MESSAGE;
        }
        storage.setDialogState(userId,
                new DialogState(DialogStep.WAITING_SEARCH_QUERY, text));
        return ASK_SEARCH_QUERY_MESSAGE;
    }

    /**
     * Ищет новости по ключевым словам, присланным в диалоге, и завершает
     * диалог. Если слова не присланы, диалог остаётся на том же шаге.
     */
    private Answer searchQueryAnswer(long userId, String scope, String text) {
        if (text.isBlank()) {
            return new Answer(SEARCH_QUERY_ERROR_MESSAGE);
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return searchAnswer(userId, scope, text);
    }

    /**
     * Ищет новости пользователя по ключевым словам: во всех лентах, если
     * область поиска — all, иначе в ленте с указанным названием. Показывает
     * первую страницу результатов с учётом размера страницы пользователя.
     */
    Answer searchAnswer(long userId, String scope, String query) {
        if (query.isBlank()) {
            return new Answer(SEARCH_QUERY_ERROR_MESSAGE);
        }
        List<Feed> feeds;
        if (scope.equalsIgnoreCase("all")) {
            feeds = storage.getAll(userId);
        } else {
            Feed feed = findFeedByName(userId, scope);
            if (feed == null) {
                return new Answer("Такой ленты нет в списке");
            }
            feeds = List.of(feed);
        }
        if (feeds.isEmpty()) {
            return new Answer(NO_SUBSCRIPTIONS_MESSAGE);
        }
        List<SearchResult> results;
        try {
            results = search.search(feeds, query);
        } catch (IOException e) {
            return new Answer(SEARCH_READ_ERROR_MESSAGE);
        }
        if (results.isEmpty()) {
            return new Answer(NO_MATCHES_MESSAGE);
        }
        return showSearchPage(userId, results, 0);
    }

    /**
     * Удаляет результаты предыдущего поиска пользователя.
     */
    void clearSearch(long userId) {
        searchPages.remove(userId);
    }

    /**
     * Показывает следующую страницу по кнопке последней выданной страницы.
     */
    Answer showMoreSearchResults(long userId, String data) {
        SearchPage page = searchPages.get(userId);
        if (page == null || !page.buttonData().equals(data) || !searchPages.remove(userId, page)) {
            return new Answer("Эта кнопка больше не действует");
        }
        return showSearchPage(userId, page.results(), page.nextIndex());
    }

    /**
     * Показывает страницу поиска и добавляет кнопку, если есть ещё результаты.
     */
    private Answer showSearchPage(long userId, List<SearchResult> results, int from) {
        int to = Math.min(from + storage.getPageSize(userId), results.size());
        String text = formatResults(results, from, to);
        if (to == results.size()) {
            return new Answer(text);
        }
        String buttonData = SEARCH_MORE_PREFIX + UUID.randomUUID();
        searchPages.put(userId, new SearchPage(results, to, buttonData));
        return new Answer(text, List.of(new Button(MORE_TEXT, buttonData)));
    }

    /**
     * Возвращает результаты с from до to, сохраняя общую нумерацию.
     */
    private String formatResults(List<SearchResult> results, int from, int to) {
        List<String> lines = new ArrayList<>();
        for (int i = from; i < to; i++) {
            SearchResult result = results.get(i);
            String source = result.feed().name().isEmpty()
                    ? ""
                    : "«" + result.feed().name() + "» ";
            lines.add((i + 1) + ". " + source + result.item().getTitle()
                    + " — " + result.item().getLink());
        }
        return String.join("\n", lines);
    }

    /**
     * Состояние перелистывания результатов поиска.
     *
     * @param results результаты одного поиска
     * @param nextIndex номер первой записи следующей страницы, начиная с нуля
     * @param buttonData данные кнопки, которая открывает следующую страницу
     */
    private record SearchPage(List<SearchResult> results, int nextIndex, String buttonData) {
    }

    /**
     * Возвращает ленту пользователя с указанным названием (без учёта
     * регистра) или null, если такой ленты нет в списке.
     */
    Feed findFeedByName(long userId, String name) {
        for (Feed feed : storage.getAll(userId)) {
            if (!feed.name().isEmpty() && feed.name().equalsIgnoreCase(name)) {
                return feed;
            }
        }
        return null;
    }

    /**
     * Задаёт размер страницы новостей пользователя. Возвращает false, если
     * размер указан неверно.
     */
    boolean changePageSize(long userId, String argument) {
        int size;
        try {
            size = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            return false;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            return false;
        }
        storage.setPageSize(userId, size);
        return true;
    }

    /**
     * Возвращает true, если по ссылке удалось прочитать ленту.
     */
    boolean isFeed(String url) {
        try {
            reader.read(url);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Возвращает ссылку на ленту пользователя с указанным названием (без учёта
     * регистра), а если такой ленты нет — переданную строку.
     */
    String findFeedUrl(long userId, String nameOrUrl) {
        for (Feed feed : storage.getAll(userId)) {
            if (!feed.name().isEmpty() && feed.name().equalsIgnoreCase(nameOrUrl)) {
                return feed.url();
            }
        }
        return nameOrUrl;
    }

    /**
     * Показывает страницу новостей ленты, начиная с позиции from, и
     * запоминает, где пользователь остановился.
     */
    Answer showPage(long userId, String url, int from) {
        List<FeedItem> news;
        try {
            news = reader.read(url);
        } catch (IOException e) {
            return new Answer("Не удалось прочитать ленту");
        }
        if (news.isEmpty()) {
            return new Answer("В ленте нет новостей");
        }
        if (from >= news.size()) {
            return new Answer("Больше новостей нет");
        }
        int to = Math.min(from + storage.getPageSize(userId), news.size());
        storage.setFeedPosition(userId, new FeedPosition(url, to));
        String text = formatNews(news, from, to);
        if (to < news.size()) {
            return new Answer(text, List.of(new Button(MORE_TEXT, MORE_DATA)));
        }
        return new Answer(text);
    }

    /**
     * Возвращает нумерованный список новостей с позиции from до позиции to.
     */
    private String formatNews(List<FeedItem> news, int from, int to) {
        List<String> lines = new ArrayList<>();
        for (int i = from; i < to; i++) {
            FeedItem item = news.get(i);
            lines.add((i + 1) + ". " + item.getTitle() + " — " + item.getLink());
        }
        return String.join("\n", lines);
    }

    /**
     * Возвращает true, если у пользователя уже есть лента с такой ссылкой.
     */
    private boolean hasFeed(long userId, String url) {
        for (Feed feed : storage.getAll(userId)) {
            if (feed.url().equals(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает true, если у пользователя уже есть лента с таким названием
     * (без учёта регистра).
     */
    private boolean hasFeedName(long userId, String name) {
        for (Feed feed : storage.getAll(userId)) {
            if (feed.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает час для показа пользователю в виде "08:00".
     */
    String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }

    /**
     * Возвращает час по строке, если это число от 0 до 23, иначе null.
     */
    Integer parseHour(String text) {
        if (!text.matches("\\d{1,2}")) {
            return null;
        }
        int hour = Integer.parseInt(text);
        return hour <= 23 ? hour : null;
    }

    /**
     * Обработчик ответа одного шага диалога.
     */
    @FunctionalInterface
    private interface StepAnswer {

        /**
         * Возвращает ответ на сообщение пользователя на шаге диалога.
         */
        Answer handle(long userId, DialogState state, String text);
    }
}
