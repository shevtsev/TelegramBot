package com.example;

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
    private final int maxPageSize = 20;

    /**
     * Подпись кнопки "Показать ещё".
     */
    private final String moreText = "Показать ещё";

    /**
     * Данные кнопки "Показать ещё".
     */
    private final String moreData = "more";

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
            case "/start" -> new Answer("Этот бот собирает новости из RSS-лент. Список команд: /help");
            case "/help" -> new Answer("""
                    Доступные команды:
                    /start - начать
                    /help - список команд
                    /add <url> - добавить ленту
                    /remove <url> - удалить ленту
                    /list - список лент
                    /latest <url> - последние новости ленты
                    /pagesize <n> - сколько новостей показывать за раз""");
            case "/add" -> new Answer(addFeed(userId, argument));
            case "/remove" -> new Answer(removeFeed(userId, argument));
            case "/list" -> new Answer(listFeeds(userId));
            case "/latest" -> latest(userId, argument);
            case "/pagesize" -> new Answer(setPageSize(userId, argument));
            default -> new Answer("Неизвестная команда. Список команд: /help");
        };
        return answer;
    }

    /**
     * Возвращает ответ на нажатие кнопки по её данным.
     */
    public Answer callbackAnswer(long userId, String data) {
        if (moreData.equals(data)) {
            return moreAnswer(userId);
        }
        return new Answer("");
    }

    /**
     * Возвращает следующую страницу новостей, когда пользователь нажал кнопку
     * "Показать ещё".
     */
    private Answer moreAnswer(long userId) {
        Reading reading = storage.getReading(userId);
        if (reading == null) {
            return new Answer("Сначала выберите ленту: /latest <url>");
        }
        return showPage(userId, reading.url(), reading.offset());
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
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(i + 1).append(". ").append(urls.get(i));
        }
        return result.toString();
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
     * Показывает страницу новостей ленты, начиная с позиции offset, и
     * запоминает, где пользователь остановился.
     */
    private Answer showPage(long userId, String url, int offset) {
        List<FeedItem> news;
        try {
            news = reader.read(url);
        } catch (IOException e) {
            return new Answer("Не удалось прочитать ленту");
        }
        if (news.isEmpty()) {
            return new Answer("В ленте нет новостей");
        }
        if (offset >= news.size()) {
            return new Answer("Больше новостей нет");
        }
        int end = Math.min(offset + storage.getPageSize(userId), news.size());
        List<String> lines = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            lines.add((i + 1) + ". " + news.get(i).getTitle() + " — " + news.get(i).getLink());
        }
        storage.setReading(userId, new Reading(url, end));
        String text = String.join("\n", lines);
        if (end < news.size()) {
            return new Answer(text, moreText, moreData);
        }
        return new Answer(text);
    }

    /**
     * Задаёт размер страницы новостей пользователя.
     */
    private String setPageSize(long userId, String argument) {
        int size;
        try {
            size = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            return "Размер страницы должен быть числом от 1 до " + maxPageSize;
        }
        if (size < 1 || size > maxPageSize) {
            return "Размер страницы должен быть числом от 1 до " + maxPageSize;
        }
        storage.setPageSize(userId, size);
        return "Размер страницы изменён: " + size;
    }
}
