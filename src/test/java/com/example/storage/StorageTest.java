package com.example.storage;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Общие тесты для хранилищ данных пользователей: каждое хранилище должно вести
 * себя одинаково. Наследники только создают проверяемое хранилище.
 */
public abstract class StorageTest {

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
     * Создаёт новое пустое хранилище для проверки.
     */
    protected abstract Storage createStorage();

    /**
     * Берёт пустое хранилище перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        storage = createStorage();
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

    /**
     * Тестирование размера страницы по умолчанию.
     */
    @Test
    void testDefaultPageSize() {
        Assertions.assertEquals(3, storage.getPageSize(USER_ID));
    }

    /**
     * Тестирование смены размера страницы: у каждого пользователя свой размер.
     */
    @Test
    void testPageSizeIsPerUser() {
        storage.setPageSize(USER_ID, 5);
        Assertions.assertEquals(5, storage.getPageSize(USER_ID));
        Assertions.assertEquals(3, storage.getPageSize(OTHER_USER_ID));
        storage.setPageSize(USER_ID, 7);
        Assertions.assertEquals(7, storage.getPageSize(USER_ID));
    }

    /**
     * Тестирование того, что у нового пользователя нет позиции в ленте.
     */
    @Test
    void testNoFeedPositionByDefault() {
        Assertions.assertNull(storage.getFeedPosition(USER_ID));
    }

    /**
     * Тестирование сохранения позиции в ленте: у каждого пользователя своя
     * позиция, новая позиция заменяет прежнюю.
     */
    @Test
    void testFeedPositionIsPerUser() {
        storage.setFeedPosition(USER_ID, new FeedPosition(FEED_URL, 3));
        Assertions.assertEquals(new FeedPosition(FEED_URL, 3), storage.getFeedPosition(USER_ID));
        Assertions.assertNull(storage.getFeedPosition(OTHER_USER_ID));
        storage.setFeedPosition(USER_ID, new FeedPosition(FEED_URL, 6));
        Assertions.assertEquals(new FeedPosition(FEED_URL, 6), storage.getFeedPosition(USER_ID));
    }

    /**
     * Тестирование настроек рассылки по умолчанию: интервал 30 минут, тихие
     * часы не настроены.
     */
    @Test
    void testDefaultMailingSettings() {
        Assertions.assertEquals(MailingSettings.DEFAULT, storage.getMailingSettings(USER_ID));
        Assertions.assertEquals(30, storage.getMailingSettings(USER_ID).checkIntervalMinutes());
        Assertions.assertNull(storage.getMailingSettings(USER_ID).quietFromHour());
        Assertions.assertNull(storage.getMailingSettings(USER_ID).quietToHour());
    }

    /**
     * Тестирование настроек рассылки: у каждого пользователя свои настройки,
     * новые настройки заменяют прежние, тихие часы можно выключить.
     */
    @Test
    void testMailingSettingsIsPerUser() {
        MailingSettings quiet = new MailingSettings(60, 23, 8);
        storage.setMailingSettings(USER_ID, quiet);
        Assertions.assertEquals(quiet, storage.getMailingSettings(USER_ID));
        Assertions.assertEquals(MailingSettings.DEFAULT, storage.getMailingSettings(OTHER_USER_ID));
        MailingSettings changed = new MailingSettings(15, 1, 7);
        storage.setMailingSettings(USER_ID, changed);
        Assertions.assertEquals(changed, storage.getMailingSettings(USER_ID));
        storage.setMailingSettings(USER_ID, MailingSettings.DEFAULT);
        Assertions.assertEquals(MailingSettings.DEFAULT, storage.getMailingSettings(USER_ID));
    }

    /**
     * Тестирование списка пользователей: пустой у нового хранилища, в списке
     * только пользователи с лентами.
     */
    @Test
    void testGetUserIds() {
        Assertions.assertTrue(storage.getUserIds().isEmpty());
        storage.add(USER_ID, new Feed("Хабр", FEED_URL));
        storage.add(USER_ID, new Feed("", "https://example.com/rss"));
        storage.add(OTHER_USER_ID, new Feed("", "https://other.com/rss"));
        Assertions.assertEquals(
                List.of(USER_ID, OTHER_USER_ID).stream().sorted().toList(),
                storage.getUserIds().stream().sorted().toList());
    }

    /**
     * Тестирование учёта отправленных новостей: новость помечается один раз,
     * новости одного пользователя не влияют на другого.
     */
    @Test
    void testSentNews() {
        String newsUrl = "https://habr.com/news/1";
        Assertions.assertFalse(storage.isNewsSent(USER_ID, newsUrl));
        Assertions.assertTrue(storage.addSentNews(USER_ID, newsUrl));
        Assertions.assertTrue(storage.isNewsSent(USER_ID, newsUrl));
        Assertions.assertFalse(storage.addSentNews(USER_ID, newsUrl));
        Assertions.assertFalse(storage.isNewsSent(OTHER_USER_ID, newsUrl));
        Assertions.assertTrue(storage.addSentNews(OTHER_USER_ID, newsUrl));
        Assertions.assertTrue(storage.isNewsSent(OTHER_USER_ID, newsUrl));
    }

    /**
     * Тестирование того, что изменение одной настройки не портит остальные.
     */
    @Test
    void testSettingsDoNotAffectEachOther() {
        DialogState waitingName = new DialogState(DialogStep.WAITING_NAME, FEED_URL);
        MailingSettings quiet = new MailingSettings(120, 22, 9);
        storage.setPageSize(USER_ID, 5);
        storage.setFeedPosition(USER_ID, new FeedPosition(FEED_URL, 3));
        storage.setDialogState(USER_ID, waitingName);
        storage.setMailingSettings(USER_ID, quiet);
        storage.addSentNews(USER_ID, "https://habr.com/news/1");
        Assertions.assertEquals(5, storage.getPageSize(USER_ID));
        Assertions.assertEquals(new FeedPosition(FEED_URL, 3), storage.getFeedPosition(USER_ID));
        Assertions.assertEquals(waitingName, storage.getDialogState(USER_ID));
        Assertions.assertEquals(quiet, storage.getMailingSettings(USER_ID));
        Assertions.assertTrue(storage.isNewsSent(USER_ID, "https://habr.com/news/1"));
    }
}
