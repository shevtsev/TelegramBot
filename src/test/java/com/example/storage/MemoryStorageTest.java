package com.example.storage;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Тесты для хранилища данных пользователей в памяти
 */
public class MemoryStorageTest {

    /**
     * Идентификатор первого пользователя.
     */
    private static final long USER_ID = 1;

    /**
     * Идентификатор второго пользователя.
     */
    private static final long OTHER_USER_ID = 2;

    /**
     * Ссылка на тестовую ленту.
     */
    private static final String FEED_URL = "https://habr.com/rss";

    /**
     * Хранилище, которое проверяется в тестах.
     */
    private Storage storage;

    /**
     * Создаёт пустое хранилище перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        storage = new MemoryStorage();
    }

    /**
     * Тестирование добавления ленты с названием и без названия.
     */
    @Test
    void testAddFeeds() {
        Feed named = new Feed("Хабр", FEED_URL);
        Feed unnamed = new Feed("", "https://example.com/rss");
        Assertions.assertTrue(storage.add(USER_ID, named));
        Assertions.assertTrue(storage.add(USER_ID, unnamed));
        Assertions.assertEquals(List.of(named, unnamed), storage.getAll(USER_ID));
    }

    /**
     * Тестирование того, что лента с такой же ссылкой не добавляется второй
     * раз, даже если название другое.
     */
    @Test
    void testAddDuplicateByUrl() {
        storage.add(USER_ID, new Feed("Хабр", FEED_URL));
        Assertions.assertFalse(storage.add(USER_ID, new Feed("Другое название", FEED_URL)));
        Assertions.assertEquals(1, storage.getAll(USER_ID).size());
    }

    /**
     * Тестирование удаления ленты по ссылке: удаляется только выбранная лента.
     */
    @Test
    void testRemove() {
        Feed removed = new Feed("Хабр", FEED_URL);
        Feed kept = new Feed("", "https://example.com/rss");
        storage.add(USER_ID, removed);
        storage.add(USER_ID, kept);
        Assertions.assertTrue(storage.remove(USER_ID, FEED_URL));
        Assertions.assertEquals(List.of(kept), storage.getAll(USER_ID));
    }

    /**
     * Тестирование удаления ленты, которой нет в списке.
     */
    @Test
    void testRemoveMissingFeed() {
        Feed kept = new Feed("Хабр", FEED_URL);
        storage.add(USER_ID, kept);
        Assertions.assertFalse(storage.remove(USER_ID, "https://example.com/rss"));
        Assertions.assertEquals(List.of(kept), storage.getAll(USER_ID));
    }

    /**
     * Тестирование того, что у разных пользователей разные ленты.
     */
    @Test
    void testUsersHaveSeparateFeeds() {
        storage.add(USER_ID, new Feed("Хабр", FEED_URL));
        Assertions.assertTrue(storage.getAll(OTHER_USER_ID).isEmpty());
    }

    /**
     * Тестирование того, что у нового пользователя нет диалога добавления
     * ленты.
     */
    @Test
    void testNoDialogByDefault() {
        Assertions.assertEquals(DialogState.NO_DIALOG, storage.getDialogState(USER_ID));
    }

    /**
     * Тестирование сохранения шага диалога: у каждого пользователя свой шаг.
     */
    @Test
    void testDialogStateIsPerUser() {
        DialogState waitingName = new DialogState(DialogStep.WAITING_NAME, FEED_URL);
        storage.setDialogState(USER_ID, waitingName);
        Assertions.assertEquals(waitingName, storage.getDialogState(USER_ID));
        Assertions.assertEquals(DialogState.NO_DIALOG, storage.getDialogState(OTHER_USER_ID));
    }
}
