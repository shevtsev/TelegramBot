package com.example.storage;

import java.util.List;

/**
 * Интерфейс хранилища данных пользователей.
 */
public interface Storage {
    /**
     * Добавляет ленту пользователю. Возвращает false, если лента уже есть
     * в списке.
     */
    boolean add(long userId, String url);

    /**
     * Удаляет ленту у пользователя. Возвращает false, если такой ленты нет
     * в списке.
     */
    boolean remove(long userId, String url);

    /**
     * Возвращает ленты пользователя в порядке добавления.
     */
    List<String> getAll(long userId);

    /**
     * Возвращает размер страницы новостей пользователя.
     */
    int getPageSize(long userId);

    /**
     * Задаёт размер страницы новостей пользователя.
     */
    void setPageSize(long userId, int size);

    /**
     * Возвращает позицию в ленте, на которой остановился пользователь, или
     * null, если он ещё не открывал ленты.
     */
    FeedPosition getFeedPosition(long userId);

    /**
     * Запоминает позицию в ленте, на которой остановился пользователь.
     */
    void setFeedPosition(long userId, FeedPosition position);
}
