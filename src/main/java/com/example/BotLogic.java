package com.example;

import java.util.List;

/**
 * Класс логики бота.
 */
public class BotLogic {

    private final Storage storage;

    public BotLogic(Storage storage) {
        this.storage = storage;
    }

    /**
     * Возвращает ответ на сообщение пользователя, либо вызывает команду, если
     * пользователь написал команду.
     */
    public String userAnswer(long userId, String text) {
        String[] parts = text.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        String answer = switch (parts[0]) {
            case "/start" -> "Этот бот собирает новости из RSS-лент. Список команд: /help";
            case "/help" -> """
                    Доступные команды:
                    /start - начать
                    /help - список команд
                    /add <url> - добавить ленту
                    /remove <url> - удалить ленту
                    /list - список лент""";
            case "/add" -> addFeed(userId, argument);
            case "/remove" -> removeFeed(userId, argument);
            case "/list" -> listFeeds(userId);
            default -> "Неизвестная команда. Список команд: /help";
        };
        return answer;
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
}
