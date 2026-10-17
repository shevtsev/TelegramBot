package com.example;

/**
 * Ответ бота: текст и признак того, что есть ещё новости
 */
public record Answer(String text, boolean hasMore) {

    /**
     * Создаёт ответ без кнопки "Показать ещё".
     */
    public Answer(String text) {
        this(text, false);
    }
}
