package com.example.storage;

/**
 * Шаг диалога добавления ленты.
 */
public enum DialogStep {

    /**
     * Пользователь не добавляет ленту.
     */
    NONE,

    /**
     * Бот ждёт ссылку на ленту.
     */
    WAITING_URL,

    /**
     * Бот ждёт название ленты.
     */
    WAITING_NAME
}
