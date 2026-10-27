package com.example;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Класс телеграмм клиента.
 */
public class BotMain extends DefaultLongPollingUpdateConsumer {

    /**
     * Клиент для отправки сообщений в Telegram.
     */
    private final TelegramClient telegramClient;

    /**
     * Логика бота.
     */
    private final BotLogic botLogic;

    /**
     * Создаёт телеграмм клиента с указанным токеном бота.
     */
    public BotMain(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.botLogic = new BotLogic(new MemoryStorage(), new HttpReader());
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            consumeCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();
        sendAnswer(chatId, botLogic.userAnswer(userId, text));
    }

    /**
     * Обрабатывает нажатие кнопки "Показать ещё".
     */
    private void consumeCallback(CallbackQuery callback) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callback.getId())
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        long chatId = callback.getMessage().getChatId();
        long userId = callback.getFrom().getId();
        sendAnswer(chatId, botLogic.callbackAnswer(userId, callback.getData()));
    }

    /**
     * Отправляет ответ бота, добавляя кнопку "Показать ещё", если есть
     * следующая страница новостей.
     */
    private void sendAnswer(long chatId, Answer answer) {
        if (answer.text().isEmpty()) {
            return;
        }
        SendMessage.SendMessageBuilder<?, ?> message = SendMessage.builder()
                .chatId(chatId)
                .text(answer.text());
        if (answer.hasMore()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(answer.buttonText())
                    .callbackData(answer.buttonData())
                    .build();
            message.replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(button))
                    .build());
        }
        try {
            telegramClient.execute(message.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
