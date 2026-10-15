package com.example;

import java.util.List;

/**
 * Интерфейс хранилища подписок пользователей.
 */
public interface Storage {
    /**
     * Добавляет ленту пользователю. Возвращает false, если лента уже есть в
     * списке.
     */
    boolean add(long userId, String url);

    /**
     * Удаляет ленту у пользователя. Возвращает false, если такой ленты нет в
     * списке.
     */
    boolean remove(long userId, String url);

    /**
     * Возвращает ленты пользователя в порядке добавления.
     */
    List<String> getAll(long userId);
}
