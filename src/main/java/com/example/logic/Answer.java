package com.example;

/**
 * Ответ бота: текст, а также подпись и данные кнопки "Показать ещё" (null,
 * если кнопки нет)
 */
public record Answer(String text, String buttonText, String buttonData) {

    /**
     * Создаёт ответ без кнопки "Показать ещё".
     */
    public Answer(String text) {
        this(text, null, null);
    }

    /**
     * Возвращает true, если к ответу нужна кнопка "Показать ещё".
     */
    public boolean hasMore() {
        return buttonData != null;
    }
}
