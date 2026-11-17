package com.example.mailing;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фейковое чтение лент: новости задаются по ссылке на ленту.
 */
class FakeReader implements NewsReader {

    /**
     * Новости лент по ссылкам.
     */
    private final Map<String, List<FeedItem>> news = new HashMap<>();

    /**
     * Задаёт новости ленты по ссылке.
     */
    void setNews(String url, FeedItem... items) {
        news.put(url, List.of(items));
    }

    @Override
    public List<FeedItem> read(String url) throws IOException {
        return news.getOrDefault(url, List.of());
    }
}
