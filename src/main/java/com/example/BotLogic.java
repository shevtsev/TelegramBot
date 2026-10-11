package com.example;

/**
 * Класс логики бота.
 */
public class BotLogic {

    private final String botName;

    public BotLogic(String botName) {
        this.botName = botName;
    }

    /**
     * Возвращает ответ на сообщение пользователя, либо вызывает команду, если
     * пользователь написал команду. Если команда адресована другому боту, возвращает пустую строку.
     */
    public String userAnswer(String text) {
        if (!text.startsWith("/")) {
            return "Пользователь написал: " + text;
        }
        String[] parts = text.split("@", 2);
        String command = parts[0];
        if (parts.length == 2 && !parts[1].equalsIgnoreCase(botName)) {
            return "";
        }
        return switch (command) {
            case "/start" -> "Этот бот повторяет всё, что ты напишешь. Список команд: /help";
            case "/help" -> """
                    Доступные команды:
                    /start - начать
                    /help - список команд
                    Любой другой текст повторяется""";
            default -> "Пользователь написал: " + text;
        };
    }
}
