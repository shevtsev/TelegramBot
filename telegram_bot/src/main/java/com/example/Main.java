package com.example;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
/**
 * Main
 */
public class Main {
    /**
     * Основная функция запуска бота.
     */
    public static void main(String[] args) throws Exception {
        String botToken = System.getenv("BOT_TOKEN");
        try (TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication()) {
            app.registerBot(botToken, new BotMain(botToken));
            System.out.println();
            Thread.currentThread().join();
        }
    }
}
