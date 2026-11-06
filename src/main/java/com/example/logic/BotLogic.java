package com.example.logic;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import com.example.storage.FeedPosition;
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
    private static final String START_MESSAGE =
            "Этот бот собирает новости из RSS-лент. Список команд: /help";

    /**
     * Ответ на команду /help.
     */
    private static final String HELP_MESSAGE = """
            Доступные команды:
            /start - начать
            /help - список команд
            /add <url> - добавить ленту
            /remove <url> - удалить ленту
            /list - список лент
            /latest <url> - последние новости ленты
            /pagesize <n> - сколько новостей показывать за раз""";

    /**
     * Ответ на неизвестную команду.
     */
    private static final String UNKNOWN_MESSAGE =
            "Неизвестная команда. Список команд: /help";

    /**
     * Ответ на неверный размер страницы.
     */
    private static final String PAGE_SIZE_ERROR_MESSAGE =
            "Размер страницы должен быть числом от 1 до " + MAX_PAGE_SIZE;

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
     * пользователь написал команду.
     */
    public Answer userAnswer(long userId, String text) {
        String[] parts = text.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        Answer answer = switch (parts[0]) {
            case "/start" -> new Answer(START_MESSAGE);
            case "/help" -> new Answer(HELP_MESSAGE);
            case "/add" -> new Answer(addFeed(userId, argument));
            case "/remove" -> new Answer(removeFeed(userId, argument));
            case "/list" -> new Answer(listFeeds(userId));
            case "/latest" -> latest(userId, argument);
            case "/pagesize" -> new Answer(pageSizeAnswer(userId, argument));
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
     * Добавляет ленту в список пользователя.
     */
    private String addFeed(long userId, String url) {
        if (url.isEmpty()) {
            return "Укажите ссылку: /add <url>";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "Не удалось найти рассылку";
        }
        if (!storage.add(userId, url)) {
            return "Эта лента уже есть в списке";
        }
        return "Лента добавлена";
    }

    /**
     * Удаляет ленту из списка пользователя.
     */
    private String removeFeed(long userId, String url) {
        if (url.isEmpty()) {
            return "Укажите ссылку: /remove <url>";
        }
        if (!storage.remove(userId, url)) {
            return "Такой ленты нет в списке";
        }
        return "Лента удалена";
    }

    /**
     * Возвращает нумерованный список лент пользователя.
     */
    private String listFeeds(long userId) {
        List<String> urls = storage.getAll(userId);
        if (urls.isEmpty()) {
            return "Подписок пока нет. Добавь ленту командой /add <url>";
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            lines.add((i + 1) + ". " + urls.get(i));
        }
        return String.join("\n", lines);
    }

    /**
     * Показывает первую страницу новостей ленты.
     */
    private Answer latest(long userId, String url) {
        if (url.isEmpty()) {
            return new Answer("Укажите ссылку: /latest <url>");
        }
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
        if (!changePageSize(userId, argument)) {
            return PAGE_SIZE_ERROR_MESSAGE;
        }
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
}
