package com.example.logic;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import com.example.storage.DialogState;
import com.example.storage.DialogStep;
import com.example.storage.Feed;
import com.example.storage.FeedPosition;
import com.example.storage.MailingSettings;
import com.example.storage.Storage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс логики бота.
 */
public class BotLogic {

    /**
     * Максимальный размер страницы новостей.
     */
    private static final int MAX_PAGE_SIZE = 20;

    /**
     * Подпись кнопки "Показать ещё".
     */
    private static final String MORE_TEXT = "Показать ещё";

    /**
     * Данные кнопки "Показать ещё".
     */
    private static final String MORE_DATA = "more";

    /**
     * Ответ на команду /start.
     */
    private static final String START_MESSAGE = "Этот бот собирает новости из RSS-лент. Список команд: /help";

    /**
     * Ответ на команду /help.
     */
    private static final String HELP_MESSAGE = """
            Доступные команды:
            /start - начать
            /help - список команд
            /add - добавить ленту по шагам
            /add <url> - добавить ленту сразу
            /cancel - отменить текущий диалог
            /remove - удалить ленту по шагам
            /remove <название или url> - удалить ленту сразу
            /list - список лент
            /latest - показать новости по шагам
            /latest <название или url> - последние новости ленты
            /pagesize - задать размер страницы по шагам
            /pagesize <n> - сколько новостей показывать за раз
            /interval - задать интервал проверки по шагам
            /interval <время> - как часто проверять ленты (например: 30, 15m, 2h, 1d)
            /quiet - задать тихие часы по шагам
            /quiet <с> <до> - задать тихие часы сразу
            /quiet off - отключить тихие часы
            /settings - текущие настройки рассылки""";

    /**
     * Ответ на неизвестную команду.
     */
    private static final String UNKNOWN_MESSAGE = "Неизвестная команда. Список команд: /help";

    /**
     * Ответ, когда по ссылке не удалось прочитать ленту.
     */
    private static final String NOT_FOUND_MESSAGE = "Не удалось найти рассылку";

    /**
     * Ответ, когда лента с такой ссылкой уже есть в списке.
     */
    private static final String DUPLICATE_MESSAGE = "Эта лента уже есть в списке";

    /**
     * Просьба прислать ссылку в диалоге добавления ленты.
     */
    private static final String ASK_URL_MESSAGE = "Пришлите ссылку на ленту";

    /**
     * Ответ на неверную ссылку в диалоге добавления ленты.
     */
    private static final String URL_ERROR_MESSAGE = NOT_FOUND_MESSAGE + ". Пришлите ссылку ещё раз или /cancel";

    /**
     * Ответ на неверный размер страницы.
     */
    private static final String PAGE_SIZE_ERROR_MESSAGE = "Размер страницы должен быть числом от 1 до " + MAX_PAGE_SIZE;

    /**
     * Просьба прислать интервал проверки лент.
     */
    private static final String ASK_INTERVAL_MESSAGE =
            "Как часто проверять ленты? Пришлите интервал, например: 30, 15m, 2h, 1d";

    /**
     * Ответ на неверный интервал проверки лент.
     */
    private static final String INTERVAL_ERROR_MESSAGE =
            "Интервал должен быть от 1 секунды до 7 дней, например: 30, 15m, 2h, 1d";

    /**
     * Просьба прислать название ленты для показа новостей.
     */
    private static final String ASK_LATEST_MESSAGE = "Новости какой ленты показать?";

    /**
     * Просьба прислать название ленты для удаления.
     */
    private static final String ASK_REMOVE_MESSAGE = "Какую ленту удалить?";

    /**
     * Просьба прислать размер страницы новостей.
     */
    private static final String ASK_PAGE_SIZE_MESSAGE =
            "Сколько новостей показывать за раз? Пришлите число от 1 до " + MAX_PAGE_SIZE;

    /**
     * Просьба прислать час начала тихих часов.
     */
    private static final String ASK_QUIET_FROM_MESSAGE =
            "С какого часа не присылать новости? Пришлите число от 0 до 23";

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
     * Хранилище данных пользователей.
     */
    private final Storage storage;

    /**
     * Чтение новостей из лент.
     */
    private final NewsReader reader;

    /**
     * Создаёт логику бота с хранилищем данных и чтением новостей из лент.
     */
    public BotLogic(Storage storage, NewsReader reader) {
        this.storage = storage;
        this.reader = reader;
    }

    /**
     * Возвращает ответ на сообщение пользователя, либо вызывает команду, если
     * пользователь написал команду. Обычный текст считается ответом на шаг
     * диалога добавления ленты.
     */
    public Answer userAnswer(long userId, String text) {
        if (!text.startsWith("/")) {
            return dialogAnswer(userId, text.trim());
        }
        String[] parts = text.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        Answer answer = switch (parts[0]) {
            case "/start" -> new Answer(START_MESSAGE);
            case "/help" -> new Answer(HELP_MESSAGE);
            case "/add" -> new Answer(addFeed(userId, argument));
            case "/cancel" -> new Answer(cancelAnswer(userId));
            case "/remove" -> new Answer(removeFeed(userId, argument));
            case "/list" -> new Answer(listFeeds(userId));
            case "/latest" -> latest(userId, argument);
            case "/pagesize" -> new Answer(pageSizeAnswer(userId, argument));
            case "/interval" -> new Answer(intervalAnswer(userId, argument));
            case "/quiet" -> new Answer(quietAnswer(userId, argument));
            case "/settings" -> new Answer(settingsAnswer(userId));
            default -> new Answer(UNKNOWN_MESSAGE);
        };
        return answer;
    }

    /**
     * Возвращает ответ на нажатие кнопки по её данным.
     */
    public Answer callbackAnswer(long userId, String data) {
        if (MORE_DATA.equals(data)) {
            return moreAnswer(userId);
        }
        return new Answer("");
    }

    /**
     * Возвращает следующую страницу новостей, когда пользователь нажал кнопку
     * "Показать ещё".
     */
    private Answer moreAnswer(long userId) {
        FeedPosition position = storage.getFeedPosition(userId);
        if (position == null) {
            return new Answer("Сначала выберите ленту: /latest <url>");
        }
        return showPage(userId, position.url(), position.shownCount());
    }

    /**
     * Добавляет ленту в список пользователя, а если ссылки нет, начинает
     * диалог добавления ленты.
     */
    private String addFeed(long userId, String url) {
        if (url.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_URL, ""));
            return ASK_URL_MESSAGE;
        }
        if (!isFeed(url)) {
            return NOT_FOUND_MESSAGE;
        }
        if (!storage.add(userId, new Feed("", url))) {
            return DUPLICATE_MESSAGE;
        }
        return "Лента добавлена";
    }

    /**
     * Возвращает ответ на обычный текст: если пользователь добавляет ленту,
     * текст считается ответом на текущий шаг диалога.
     */
    private Answer dialogAnswer(long userId, String text) {
        DialogState state = storage.getDialogState(userId);
        return switch (state.step()) {
            case WAITING_URL -> new Answer(urlAnswer(userId, text));
            case WAITING_NAME -> new Answer(nameAnswer(userId, state.url(), text));
            case WAITING_INTERVAL -> new Answer(intervalValueAnswer(userId, text));
            case WAITING_QUIET_FROM -> new Answer(quietFromAnswer(userId, text));
            case WAITING_QUIET_TO -> new Answer(quietToAnswer(userId, state.url(), text));
            case WAITING_LATEST -> latestDialogAnswer(userId, text);
            case WAITING_REMOVE -> new Answer(removeDialogAnswer(userId, text));
            case WAITING_PAGE_SIZE -> new Answer(pageSizeDialogAnswer(userId, text));
            case NONE -> new Answer(UNKNOWN_MESSAGE);
        };
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
     * Отменяет текущий диалог и возвращает ответ пользователю.
     */
    private String cancelAnswer(long userId) {
        DialogStep step = storage.getDialogState(userId).step();
        if (step == DialogStep.NONE) {
            return "Нечего отменять";
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return switch (step) {
            case WAITING_URL, WAITING_NAME -> "Добавление отменено";
            case WAITING_INTERVAL, WAITING_QUIET_FROM, WAITING_QUIET_TO -> "Настройка отменена";
            default -> "Действие отменено";
        };
    }

    /**
     * Возвращает true, если по ссылке удалось прочитать ленту.
     */
    private boolean isFeed(String url) {
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
    private String findFeedUrl(long userId, String nameOrUrl) {
        for (Feed feed : storage.getAll(userId)) {
            if (!feed.name().isEmpty() && feed.name().equalsIgnoreCase(nameOrUrl)) {
                return feed.url();
            }
        }
        return nameOrUrl;
    }

    /**
     * Удаляет ленту из списка пользователя по названию или ссылке.
     */
    private String removeFeed(long userId, String nameOrUrl) {
        if (nameOrUrl.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_REMOVE, ""));
            return ASK_REMOVE_MESSAGE;
        }
        if (!storage.remove(userId, findFeedUrl(userId, nameOrUrl))) {
            return "Такой ленты нет в списке";
        }
        return "Лента удалена";
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
     * Возвращает нумерованный список лент пользователя.
     */
    private String listFeeds(long userId) {
        List<Feed> feeds = storage.getAll(userId);
        if (feeds.isEmpty()) {
            return "Подписок пока нет. Добавь ленту командой /add <url>";
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < feeds.size(); i++) {
            lines.add((i + 1) + ". " + formatFeed(feeds.get(i)));
        }
        return String.join("\n", lines);
    }

    /**
     * Возвращает ленту для показа: название и ссылка, а если названия нет,
     * только ссылка.
     */
    private String formatFeed(Feed feed) {
        if (feed.name().isEmpty()) {
            return feed.url();
        }
        return feed.name() + " — " + feed.url();
    }

    /**
     * Показывает первую страницу новостей ленты по названию или ссылке.
     */
    private Answer latest(long userId, String nameOrUrl) {
        if (nameOrUrl.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_LATEST, ""));
            return new Answer(ASK_LATEST_MESSAGE);
        }
        return showPage(userId, findFeedUrl(userId, nameOrUrl), 0);
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
     * Показывает страницу новостей ленты, начиная с позиции from, и
     * запоминает, где пользователь остановился.
     */
    private Answer showPage(long userId, String url, int from) {
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
     * Меняет размер страницы и возвращает ответ пользователю.
     */
    private String pageSizeAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_PAGE_SIZE, ""));
            return ASK_PAGE_SIZE_MESSAGE;
        }
        if (!changePageSize(userId, argument)) {
            return PAGE_SIZE_ERROR_MESSAGE;
        }
        return "Размер страницы изменён: " + storage.getPageSize(userId);
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
     * Задаёт размер страницы новостей пользователя. Возвращает false, если
     * размер указан неверно.
     */
    private boolean changePageSize(long userId, String argument) {
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
     * Задаёт интервал проверки лент из параметра команды, а если параметра
     * нет, начинает диалог настройки интервала.
     */
    private String intervalAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_INTERVAL, ""));
            return ASK_INTERVAL_MESSAGE;
        }
        return intervalValueAnswer(userId, argument);
    }

    /**
     * Сохраняет интервал проверки лент, присланный параметром или в диалоге.
     * Если интервал указан неверно, диалог остаётся на шаге интервала.
     */
    private String intervalValueAnswer(long userId, String text) {
        Integer seconds = IntervalParser.parseSeconds(text);
        if (seconds == null) {
            return INTERVAL_ERROR_MESSAGE;
        }
        MailingSettings settings = storage.getMailingSettings(userId);
        storage.setMailingSettings(userId, new MailingSettings(
                seconds, settings.quietFromHour(), settings.quietToHour()));
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return "Буду проверять ленты " + IntervalParser.describe(seconds);
    }

    /**
     * Задаёт тихие часы из параметров команды: два часа через пробел или
     * "off" для отключения. Без параметров начинает диалог настройки тихих
     * часов.
     */
    private String quietAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId,
                    new DialogState(DialogStep.WAITING_QUIET_FROM, ""));
            return ASK_QUIET_FROM_MESSAGE;
        }
        if (argument.equals("off")) {
            MailingSettings settings = storage.getMailingSettings(userId);
            storage.setMailingSettings(userId, new MailingSettings(
                    settings.checkIntervalSeconds(), null, null));
            return "Тихие часы отключены";
        }
        String[] hours = argument.split("\\s+");
        if (hours.length == 2) {
            Integer from = parseHour(hours[0]);
            Integer to = parseHour(hours[1]);
            if (from != null && to != null) {
                return saveQuietHours(userId, from, to);
            }
        }
        return "Укажите тихие часы двумя числами от 0 до 23: /quiet <с> <до>";
    }

    /**
     * Запоминает час начала тихих часов, присланный в диалоге, и переходит
     * к шагу часа конца. Если час указан неверно, диалог остаётся на том же
     * шаге.
     */
    private String quietFromAnswer(long userId, String text) {
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
    private String quietToAnswer(long userId, String fromText, String text) {
        Integer to = parseHour(text);
        if (to == null) {
            return QUIET_HOUR_ERROR_MESSAGE;
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return saveQuietHours(userId, Integer.parseInt(fromText), to);
    }

    /**
     * Возвращает час по строке, если это число от 0 до 23, иначе null.
     */
    private Integer parseHour(String text) {
        if (!text.matches("\\d{1,2}")) {
            return null;
        }
        int hour = Integer.parseInt(text);
        return hour <= 23 ? hour : null;
    }

    /**
     * Сохраняет тихие часы пользователя и возвращает ответ пользователю.
     */
    private String saveQuietHours(long userId, int from, int to) {
        MailingSettings settings = storage.getMailingSettings(userId);
        storage.setMailingSettings(userId,
                new MailingSettings(settings.checkIntervalSeconds(), from, to));
        return "Не буду присылать новости с " + formatHour(from)
                + " до " + formatHour(to);
    }

    /**
     * Показывает текущие настройки рассылки пользователя.
     */
    private String settingsAnswer(long userId) {
        MailingSettings settings = storage.getMailingSettings(userId);
        String quiet = "не настроены";
        if (settings.quietFromHour() != null) {
            quiet = "с " + formatHour(settings.quietFromHour())
                    + " до " + formatHour(settings.quietToHour());
        }
        return "Проверка лент: "
                + IntervalParser.describe(settings.checkIntervalSeconds())
                + ". Тихие часы: " + quiet;
    }

    /**
     * Возвращает час для показа пользователю в виде "08:00".
     */
    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }
}
