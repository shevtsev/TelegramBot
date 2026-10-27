package com.example.logic;

import java.util.List;

/**
 * Ответ бота: текст и кнопки под сообщением (пустой список, если кнопок нет).
 */
public record Answer(String text, List<Button> buttons) {

    /**
     * Создаёт ответ без кнопок.
     */
    public Answer(String text) {
        this(text, List.of());
    }
}
