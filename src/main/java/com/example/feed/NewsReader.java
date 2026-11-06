package com.example.feed;

import java.io.IOException;
import java.util.List;

/**
 * Интерфейс чтения новостей из ленты.
 */
public interface NewsReader {
    /**
     * Возвращает новости ленты по ссылке. Если ленту прочитать не удалось,
     * бросает IOException.
     */
    List<FeedItem> read(String url) throws IOException;
}
