package com.example;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

    /**
     * Новости тестовой ленты.
     */
    private final List<FeedItem> NEWS = List.of(
            new FeedItem("Первая", "https://example.com/1", ""),
            new FeedItem("Вторая", "https://example.com/2", ""),
            new FeedItem("Третья", "https://example.com/3", ""),
            new FeedItem("Четвёртая", "https://example.com/4", ""),
            new FeedItem("Пятая", "https://example.com/5", ""));

    /**
     * Логика бота с тестовым хранилищем и тестовой лентой.
     */
    private final BotLogic botLogic = new BotLogic(new MemoryStorage(), url -> {
        if (url.equals("https://broken.com/rss")) {
            throw new IOException("Лента недоступна");
        }
        return url.equals("https://empty.com/rss") ? List.of() : NEWS;
    });

    /**
     * Тестирование добавления, просмотра и удаления ленты.
     */
    @Test
    void testAddListRemove() {
        Assertions.assertEquals("Лента добавлена", botLogic.userAnswer(1, "/add https://habr.com/rss").text());
        Assertions.assertEquals("1. https://habr.com/rss", botLogic.userAnswer(1, "/list").text());
        Assertions.assertEquals("Лента удалена", botLogic.userAnswer(1, "/remove https://habr.com/rss").text());
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                botLogic.userAnswer(1, "/list").text());
    }

    /**
     * Тестирование того, что у разных пользователей разные списки лент.
     */
    @Test
    void testUsersHaveSeparateSubscriptions() {
        botLogic.userAnswer(1, "/add https://habr.com/rss");
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                botLogic.userAnswer(2, "/list").text());
        Assertions.assertEquals("Такой ленты нет в списке",
                botLogic.userAnswer(2, "/remove https://habr.com/rss").text());
        Assertions.assertEquals("1. https://habr.com/rss", botLogic.userAnswer(1, "/list").text());
    }

    /**
     * Тестирование ошибок при добавлении ленты.
     */
    @Test
    void testAddErrors() {
        Assertions.assertEquals("Укажите ссылку: /add <url>", botLogic.userAnswer(1, "/add").text());
        Assertions.assertEquals("Не удалось найти рассылку", botLogic.userAnswer(1, "/add habr").text());
        botLogic.userAnswer(1, "/add https://habr.com/rss");
        Assertions.assertEquals("Эта лента уже есть в списке",
                botLogic.userAnswer(1, "/add https://habr.com/rss").text());
    }

    /**
     * Тестирование ошибок при удалении ленты.
     */
    @Test
    void testRemoveErrors() {
        Assertions.assertEquals("Укажите ссылку: /remove <url>", botLogic.userAnswer(1, "/remove").text());
        Assertions.assertEquals("Такой ленты нет в списке",
                botLogic.userAnswer(1, "/remove https://habr.com/rss").text());
    }

    /**
     * Тестирование первой страницы новостей: по умолчанию 3 новости и кнопка
     * «Показать ещё».
     */
    @Test
    void testLatestFirstPage() {
        Answer answer = botLogic.userAnswer(1, "/latest https://habr.com/rss");
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2
                3. Третья — https://example.com/3""", answer.text());
        Assertions.assertTrue(answer.hasMore());
    }

    /**
     * Тестирование следующих страниц: после последней страницы кнопки нет.
     */
    @Test
    void testShowMore() {
        Answer first = botLogic.userAnswer(1, "/latest https://habr.com/rss");
        Answer second = botLogic.callbackAnswer(1, first.buttonData());
        Assertions.assertEquals("""
                4. Четвёртая — https://example.com/4
                5. Пятая — https://example.com/5""", second.text());
        Assertions.assertFalse(second.hasMore());
        Assertions.assertEquals("Больше новостей нет", botLogic.callbackAnswer(1, first.buttonData()).text());
    }

    /**
     * Тестирование кнопки «Показать ещё» без выбранной ленты.
     */
    @Test
    void testShowMoreWithoutLatest() {
        Assertions.assertEquals("Сначала выберите ленту: /latest <url>", botLogic.callbackAnswer(1, "more").text());
    }

    /**
     * Тестирование ошибок команды /latest.
     */
    @Test
    void testLatestErrors() {
        Assertions.assertEquals("Укажите ссылку: /latest <url>", botLogic.userAnswer(1, "/latest").text());
        Assertions.assertEquals("Не удалось прочитать ленту",
                botLogic.userAnswer(1, "/latest https://broken.com/rss").text());
        Assertions.assertEquals("В ленте нет новостей",
                botLogic.userAnswer(1, "/latest https://empty.com/rss").text());
    }

    /**
     * Тестирование размера страницы.
     */
    @Test
    void testPageSize() {
        Assertions.assertEquals("Размер страницы изменён: 2", botLogic.userAnswer(1, "/pagesize 2").text());
        Answer answer = botLogic.userAnswer(1, "/latest https://habr.com/rss");
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2""", answer.text());
        Assertions.assertTrue(answer.hasMore());
    }

    /**
     * Тестирование того, что размер страницы у каждого пользователя свой.
     */
    @Test
    void testPageSizeIsPerUser() {
        botLogic.userAnswer(1, "/pagesize 1");
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2
                3. Третья — https://example.com/3""", botLogic.userAnswer(2, "/latest https://habr.com/rss").text());
    }

    /**
     * Тестирование ошибок команды /pagesize.
     */
    @Test
    void testPageSizeErrors() {
        String error = "Размер страницы должен быть числом от 1 до 20";
        Assertions.assertEquals(error, botLogic.userAnswer(1, "/pagesize").text());
        Assertions.assertEquals(error, botLogic.userAnswer(1, "/pagesize abc").text());
        Assertions.assertEquals(error, botLogic.userAnswer(1, "/pagesize 0").text());
        Assertions.assertEquals(error, botLogic.userAnswer(1, "/pagesize 21").text());
    }
}
