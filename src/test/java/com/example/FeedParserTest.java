package com.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Тесты для разбора RSS-лент из локальных XML-файлов
 */
public class FeedParserTest {

    /**
     * Парсер лент, который проверяется в тестах.
     */
    private final FeedParser feedParser = new FeedParser();

    /**
     * Тестирование разбора ленты с тремя новостями.
     */
    @Test
    void testParseItems() throws IOException {
        try (InputStream input = openResource("feed_habr.xml")) {
            List<FeedItem> items = feedParser.parse(input);
            Assertions.assertEquals(3, items.size());
            Assertions.assertEquals("Первая новость", items.get(0).getTitle());
            Assertions.assertEquals("https://habr.com/ru/articles/1/", items.get(0).getLink());
            Assertions.assertEquals("Mon, 12 Oct 2026 10:00:00 +0000", items.get(0).getPubDate());
            Assertions.assertEquals("Третья новость", items.get(2).getTitle());
        }
    }

    /**
     * Тестирование того, что лента без новостей даёт пустой список.
     */
    @Test
    void testParseEmptyFeed() throws IOException {
        try (InputStream input = openResource("feed_empty.xml")) {
            Assertions.assertTrue(feedParser.parse(input).isEmpty());
        }
    }

    /**
     * Тестирование ошибки при сломанном XML.
     */
    @Test
    void testParseBrokenFeed() throws IOException {
        try (InputStream input = openResource("feed_broken.xml")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> feedParser.parse(input));
        }
    }

    /**
     * Открывает XML-файл из папки test/resources.
     */
    private InputStream openResource(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
}
