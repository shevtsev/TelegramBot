package com.example.feed;

import com.example.storage.Feed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Проверяет поиск по новостям из локальных RSS-лент.
 */
class NewsSearchTest {

    /** Лента с новостями о языках программирования. */
    private final Feed programming = new Feed("Разработка", "/feed_search.xml");

    /** Вторая лента пользователя. */
    private final Feed habr = new Feed("Хабр", "/feed_habr.xml");

    /** Поиск, который читает ленты из ресурсов тестов. */
    private final NewsSearch search = new NewsSearch(this::readFeed);

    /** Проверяет поиск по заголовку и описанию сразу в двух лентах. */
    @Test
    void findsAnyKeywordInSelectedFeeds() throws IOException {
        List<SearchResult> results = search.search(
                List.of(programming, habr), "  JAVA  kotlin ТРЕТЬЯ  ");

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(programming, results.get(0).feed());
        Assertions.assertEquals("Вышла новая версия Java", results.get(0).item().getTitle());
        Assertions.assertEquals(programming, results.get(1).feed());
        Assertions.assertEquals("Обновление инструментов", results.get(1).item().getTitle());
        Assertions.assertEquals(habr, results.get(2).feed());
        Assertions.assertEquals("Третья новость", results.get(2).item().getTitle());
    }

    /** Проверяет, что поиск учитывает только переданные ленты. */
    @Test
    void searchesOnlySelectedFeed() throws IOException {
        Assertions.assertTrue(search.search(List.of(habr), "java").isEmpty());
    }

    /** Читает новости из локального RSS-файла. */
    private List<FeedItem> readFeed(String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Тестовая лента не найдена: " + resource);
            }
            return new FeedParser().parse(input);
        }
    }
}
