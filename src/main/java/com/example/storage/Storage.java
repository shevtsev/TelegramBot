package com.example.storage;

import java.util.List;

/**
 * Интерфейс хранилища данных пользователей.
 */
public interface Storage {

    /**
     * Размер страницы новостей по умолчанию.
     */
    int DEFAULT_PAGE_SIZE = 3;

    /**
     * Добавляет ленту пользователю. Возвращает false, если лента с такой
     * ссылкой уже есть в списке.
     */
    boolean add(long userId, Feed feed);

    /**
     * Удаляет ленту с указанной ссылкой у пользователя. Возвращает false,
     * если такой ленты нет в списке.
     */
    boolean remove(long userId, String url);

    /**
     * Возвращает ленты пользователя в порядке добавления.
     */
    List<Feed> getAll(long userId);

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

    /**
     * Возвращает состояние диалога добавления ленты у пользователя или
     * DialogState.NO_DIALOG, если пользователь не добавляет ленту.
     */
    DialogState getDialogState(long userId);

    /**
     * Запоминает состояние диалога добавления ленты у пользователя.
     */
    void setDialogState(long userId, DialogState state);

    /**
     * Возвращает настройки рассылки новостей пользователя. Если настройки не
     * заданы, возвращает MailingSettings.DEFAULT.
     */
    MailingSettings getMailingSettings(long userId);

    /**
     * Запоминает настройки рассылки новостей пользователя.
     */
    void setMailingSettings(long userId, MailingSettings settings);

    /**
     * Возвращает идентификаторы пользователей, у которых есть ленты.
     */
    List<Long> getUserIds();

    /**
     * Помечает новость как отправленную пользователю. Возвращает false, если
     * эта новость уже была отмечена раньше.
     */
    boolean addSentNews(long userId, String newsUrl);

    /**
     * Проверяет, была ли новость уже отправлена пользователю.
     */
    boolean isNewsSent(long userId, String newsUrl);
}
