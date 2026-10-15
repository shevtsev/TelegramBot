package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище подписок пользователей в памяти.
 */
public class MemoryStorage implements Storage {

    private final Map<Long, List<String>> subscriptions = new HashMap<>();

    @Override
    public synchronized boolean add(long userId, String url) {
        List<String> urls = subscriptions.computeIfAbsent(userId, id -> new ArrayList<>());
        if (urls.contains(url)) {
            return false;
        }
        urls.add(url);
        return true;
    }

    @Override
    public synchronized boolean remove(long userId, String url) {
        List<String> urls = subscriptions.get(userId);
        return urls != null && urls.remove(url);
    }

    @Override
    public synchronized List<String> getAll(long userId) {
        return new ArrayList<>(subscriptions.getOrDefault(userId, List.of()));
    }
}
