package com.example.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Все данные одного пользователя: ленты, размер страницы новостей, позиция
 * в ленте и состояние диалога добавления ленты.
 */
class UserData {

    /**
     * Ленты пользователя в порядке добавления.
     */
    private final List<Feed> feeds = new ArrayList<>();

    /**
     * Размер страницы новостей.
     */
    private int pageSize = Storage.DEFAULT_PAGE_SIZE;

    /**
     * Позиция в ленте, на которой остановился пользователь, или null.
     */
    private FeedPosition feedPosition;

    /**
     * Состояние диалога добавления ленты.
     */
    private DialogState dialogState = DialogState.NO_DIALOG;

    /**
     * Настройки рассылки новостей.
     */
    private MailingSettings mailingSettings = MailingSettings.DEFAULT;

    /**
     * Ссылки на уже отправленные новости.
     */
    private final Set<String> sentNews = new HashSet<>();

    /**
     * Возвращает ленты пользователя.
     */
    List<Feed> getFeeds() {
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

    /**
     * Возвращает состояние диалога добавления ленты.
     */
    DialogState getDialogState() {
        return dialogState;
    }

    /**
     * Запоминает состояние диалога добавления ленты.
     */
    void setDialogState(DialogState dialogState) {
        this.dialogState = dialogState;
    }

    /**
     * Возвращает настройки рассылки новостей.
     */
    MailingSettings getMailingSettings() {
        return mailingSettings;
    }

    /**
     * Запоминает настройки рассылки новостей.
     */
    void setMailingSettings(MailingSettings mailingSettings) {
        this.mailingSettings = mailingSettings;
    }

    /**
     * Возвращает ссылки на уже отправленные новости.
     */
    Set<String> getSentNews() {
        return sentNews;
    }
}
