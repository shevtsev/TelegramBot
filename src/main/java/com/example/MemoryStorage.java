package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилище подписок пользователей в памяти.
 */
public class MemoryStorage implements Storage {

    private static final int DEFAULT_PAGE_SIZE = 3;

    private final Map<Long, List<String>> subscriptions = new HashMap<>();

    private final Map<Long, Integer> pageSizes = new HashMap<>();

    private final Map<Long, Reading> readings = new HashMap<>();

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

    @Override
    public synchronized int getPageSize(long userId) {
        return pageSizes.getOrDefault(userId, DEFAULT_PAGE_SIZE);
    }

    @Override
    public synchronized void setPageSize(long userId, int size) {
        pageSizes.put(userId, size);
    }

    @Override
    public synchronized Reading getReading(long userId) {
        return readings.get(userId);
    }

    @Override
    public synchronized void setReading(long userId, Reading reading) {
        readings.put(userId, reading);
    }
}
