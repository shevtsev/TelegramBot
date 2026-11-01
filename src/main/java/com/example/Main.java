package com.example;

import com.example.storage.DbStorage;
import com.example.telegram.BotMain;
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
        try (DbStorage storage = new DbStorage(dotenv.get("DB_URL"),
                dotenv.get("DB_USER"), dotenv.get("DB_PASSWORD"));
                TelegramBotsLongPollingApplication app =
                        new TelegramBotsLongPollingApplication()) {
            app.registerBot(botToken, new BotMain(botToken, storage));
            Thread.currentThread().join();
        }
    }
}
