package com.example.storage;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для хранилища данных пользователей в базе данных. База данных
 * находится в оперативной памяти.
 */
public class DbStorageTest extends StorageTest {

    /**
     * Идентификатор пользователя.
     */
    private static final long USER_ID = 1;

    /**
     * Начало адреса базы данных в памяти. После него идёт уникальное имя базы
     * и настройки: режим PostgreSQL и сохранение базы, пока открыто хоть одно
     * соединение или пока работает программа.
     */
    private static final String URL_PREFIX = "jdbc:h2:mem:";

    /**
     * Настройки базы данных в памяти.
     */
    private static final String URL_OPTIONS = ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    /**
     * Адрес базы данных в памяти для текущего теста.
     */
    private String url;

    /**
     * Хранилище, созданное для текущего теста (закрывается после теста).
     */
    private DbStorage dbStorage;

    @Override
    protected Storage createStorage() {
        url = URL_PREFIX + UUID.randomUUID() + URL_OPTIONS;
        dbStorage = new DbStorage(url, "", "");
        return dbStorage;
    }

    /**
     * Закрывает соединение с базой данных после каждого теста.
     */
    @AfterEach
    void tearDown() {
        dbStorage.close();
    }

    /**
     * Тестирование того, что данные не пропадают при перезапуске: после
     * закрытия хранилища и нового подключения к той же базе все данные на
     * месте.
     */
    @Test
    void testDataSurvivesReconnect() {
        Feed feed = new Feed("Хабр", "https://habr.com/rss");
        DialogState waitingName = new DialogState(DialogStep.WAITING_NAME, feed.url());
        MailingSettings quiet = new MailingSettings(60, 23, 8);
        dbStorage.add(USER_ID, feed);
        dbStorage.setPageSize(USER_ID, 5);
        dbStorage.setFeedPosition(USER_ID, new FeedPosition(feed.url(), 3));
        dbStorage.setDialogState(USER_ID, waitingName);
        dbStorage.setMailingSettings(USER_ID, quiet);
        dbStorage.addSentNews(USER_ID, "https://habr.com/news/1");
        dbStorage.close();

        dbStorage = new DbStorage(url, "", "");
        Assertions.assertEquals(List.of(feed), dbStorage.getAll(USER_ID));
        Assertions.assertEquals(5, dbStorage.getPageSize(USER_ID));
        Assertions.assertEquals(new FeedPosition(feed.url(), 3),
                dbStorage.getFeedPosition(USER_ID));
        Assertions.assertEquals(waitingName, dbStorage.getDialogState(USER_ID));
        Assertions.assertEquals(quiet, dbStorage.getMailingSettings(USER_ID));
        Assertions.assertTrue(dbStorage.isNewsSent(USER_ID, "https://habr.com/news/1"));
        Assertions.assertTrue(dbStorage.hasSentNews(USER_ID));
    }

    /**
     * Тестирование порядка лент: ленты возвращаются в порядке добавления.
     */
    @Test
    void testFeedsKeepOrder() {
        Feed first = new Feed("", "https://b.com/rss");
        Feed second = new Feed("", "https://a.com/rss");
        dbStorage.add(USER_ID, first);
        dbStorage.add(USER_ID, second);
        Assertions.assertEquals(List.of(first, second), dbStorage.getAll(USER_ID));
    }

    /**
     * Тестирование ошибки при недоступной базе данных: проверяются и класс
     * исключения, и начало его текста.
     */
    @Test
    void testConnectionError() {
        StorageException exception = Assertions.assertThrows(StorageException.class,
                () -> new DbStorage("jdbc:unknown:db", "", ""));
        Assertions.assertEquals("Не удалось подключиться к базе данных", exception.getMessage());
    }
}
