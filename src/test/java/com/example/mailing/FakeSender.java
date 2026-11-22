package com.example.mailing;

import java.util.ArrayList;
import java.util.List;

/**
 * Фейковая отправка сообщений: запоминает тексты сообщений.
 */
class FakeSender implements NewsSender {

    /**
     * Отправленные сообщения.
     */
    final List<String> messages = new ArrayList<>();

    @Override
    public void sendNews(long userId, String text) {
        messages.add(text);
    }
}
