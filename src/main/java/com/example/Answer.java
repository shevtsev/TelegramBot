package com.example;

/**
 * Ответ бота: текст и признак того, что есть ещё новости (нужна кнопка
 * «Показать ещё»).
 */
public record Answer(String text, boolean hasMore) {

    public Answer(String text) {
        this(text, false);
    }
}
