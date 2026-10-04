package com.example;

import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class BotMain extends DefaultLongPollingUpdateConsumer {

    private final TelegramClient telegramClient;

    public BotMain(TelegramClient client) {
        this.telegramClient = client;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(BotLogic.UserAnswer(update.getMessage().getText()))
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
