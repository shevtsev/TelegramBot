package com.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Точка входа в приложение. Запускает бота.
 */
public class Main {
    /**
     * Основная функция запуска бота.
     */
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        String botToken = dotenv.get("BOT_TOKEN");
        String botName = dotenv.get("BOT_NAME");
        try (TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication()) {
            app.registerBot(botToken, new BotMain(botToken));
            Thread.currentThread().join();
        }
    }
}
