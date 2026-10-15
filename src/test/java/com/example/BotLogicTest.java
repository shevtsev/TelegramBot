package com.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

    private final BotLogic botLogic = new BotLogic(new MemoryStorage());

    /**
     * Тестирование добавления, просмотра и удаления ленты.
     */
    @Test
    void testAddListRemove() {
        Assertions.assertEquals("Лента добавлена", botLogic.userAnswer(1, "/add https://habr.com/rss"));
        Assertions.assertEquals("1. https://habr.com/rss", botLogic.userAnswer(1, "/list"));
        Assertions.assertEquals("Лента удалена", botLogic.userAnswer(1, "/remove https://habr.com/rss"));
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                botLogic.userAnswer(1, "/list"));
    }

    /**
     * Тестирование того, что у разных пользователей разные списки лент.
     */
    @Test
    void testUsersHaveSeparateSubscriptions() {
        botLogic.userAnswer(1, "/add https://habr.com/rss");
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                botLogic.userAnswer(2, "/list"));
        Assertions.assertEquals("Такой ленты нет в списке", botLogic.userAnswer(2, "/remove https://habr.com/rss"));
        Assertions.assertEquals("1. https://habr.com/rss", botLogic.userAnswer(1, "/list"));
    }

    /**
     * Тестирование ошибок при добавлении ленты.
     */
    @Test
    void testAddErrors() {
        Assertions.assertEquals("Укажите ссылку: /add <url>", botLogic.userAnswer(1, "/add"));
        Assertions.assertEquals("Не удалось найти рассылку", botLogic.userAnswer(1, "/add habr"));
        botLogic.userAnswer(1, "/add https://habr.com/rss");
        Assertions.assertEquals("Эта лента уже есть в списке", botLogic.userAnswer(1, "/add https://habr.com/rss"));
    }

    /**
     * Тестирование ошибок при удалении ленты.
     */
    @Test
    void testRemoveErrors() {
        Assertions.assertEquals("Укажите ссылку: /remove <url>", botLogic.userAnswer(1, "/remove"));
        Assertions.assertEquals("Такой ленты нет в списке", botLogic.userAnswer(1, "/remove https://habr.com/rss"));
    }
}
