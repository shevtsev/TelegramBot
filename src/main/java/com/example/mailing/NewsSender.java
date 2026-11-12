package com.example.mailing;

/**
 * Интерфейс отправки новостей пользователям.
 */
public interface NewsSender {

    /**
     * Отправляет сообщение пользователю.
     */
    void sendNews(long userId, String text);
}
