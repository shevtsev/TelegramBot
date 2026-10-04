package com.example;

public class BotLogic {
    /**
        * Возвращает ответ на сообщение пользователя, либо вызывает команду, если пользователь написал команду.
    */
    public static String UserAnswer(String text) {
        String answer = switch (text) {
            case "/start" -> "Этот бот повторяет всё, что ты напишешь. Список команд: /help";
            case "/help" -> """
                    Доступные команды:
                    /start - начать
                    /help - список команд
                    Любой другой текст повторяется""";
            default -> "Пользователь написал: " + text;
        };
        return answer;
    }
}
