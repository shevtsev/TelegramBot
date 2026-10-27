package com.example.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Все данные одного пользователя: ленты, размер страницы новостей и позиция
 * в ленте.
 */
class UserData {

    /**
     * Размер страницы новостей по умолчанию.
     */
    private static final int DEFAULT_PAGE_SIZE = 3;

    /**
     * Ленты пользователя в порядке добавления.
     */
    private final List<String> feeds = new ArrayList<>();

    /**
     * Размер страницы новостей.
     */
    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Позиция в ленте, на которой остановился пользователь, или null.
     */
    private FeedPosition feedPosition;

    /**
     * Возвращает ленты пользователя.
     */
    List<String> getFeeds() {
        return feeds;
    }

    /**
     * Возвращает размер страницы новостей.
     */
    int getPageSize() {
        return pageSize;
    }

    /**
     * Задаёт размер страницы новостей.
     */
    void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Возвращает позицию в ленте, на которой остановился пользователь.
     */
    FeedPosition getFeedPosition() {
        return feedPosition;
    }

    /**
     * Запоминает позицию в ленте, на которой остановился пользователь.
     */
    void setFeedPosition(FeedPosition feedPosition) {
        this.feedPosition = feedPosition;
    }
}
