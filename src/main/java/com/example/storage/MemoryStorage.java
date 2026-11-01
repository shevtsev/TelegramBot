package com.example.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище данных пользователей в памяти.
 */
public class MemoryStorage implements Storage {

    /**
     * Данные пользователей по их id.
     */
    private final Map<Long, UserData> users = new HashMap<>();

    @Override
    public boolean add(long userId, Feed feed) {
        List<Feed> feeds = getUser(userId).getFeeds();
        for (Feed existing : feeds) {
            if (existing.url().equals(feed.url())) {
                return false;
            }
        }
        feeds.add(feed);
        return true;
    }

    @Override
    public boolean remove(long userId, String url) {
        return getUser(userId).getFeeds().removeIf(feed -> feed.url().equals(url));
    }

    @Override
    public List<Feed> getAll(long userId) {
        return new ArrayList<>(getUser(userId).getFeeds());
    }

    @Override
    public int getPageSize(long userId) {
        return getUser(userId).getPageSize();
    }

    @Override
    public void setPageSize(long userId, int size) {
        getUser(userId).setPageSize(size);
    }

    @Override
    public FeedPosition getFeedPosition(long userId) {
        return getUser(userId).getFeedPosition();
    }

    @Override
    public void setFeedPosition(long userId, FeedPosition position) {
        getUser(userId).setFeedPosition(position);
    }

    @Override
    public DialogState getDialogState(long userId) {
        return getUser(userId).getDialogState();
    }

    @Override
    public void setDialogState(long userId, DialogState state) {
        getUser(userId).setDialogState(state);
    }

    /**
     * Возвращает данные пользователя, создавая их при первом обращении.
     */
    private UserData getUser(long userId) {
        return users.computeIfAbsent(userId, id -> new UserData());
    }
}
