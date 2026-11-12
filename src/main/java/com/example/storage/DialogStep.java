package com.example.storage;

/**
 * Шаг диалога с пользователем.
 */
public enum DialogStep {

    /**
     * Пользователь не ведёт диалог с ботом.
     */
    NONE,

    /**
     * Бот ждёт ссылку на ленту.
     */
    WAITING_URL,

    /**
     * Бот ждёт название ленты.
     */
    WAITING_NAME,

    /**
     * Бот ждёт интервал проверки лент.
     */
    WAITING_INTERVAL,

    /**
     * Бот ждёт час начала тихих часов.
     */
    WAITING_QUIET_FROM,

    /**
     * Бот ждёт час конца тихих часов.
     */
    WAITING_QUIET_TO
}
