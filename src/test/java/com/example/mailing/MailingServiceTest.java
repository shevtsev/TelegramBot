package com.example.mailing;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import com.example.storage.Feed;
import com.example.storage.MailingSettings;
import com.example.storage.MemoryStorage;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Тесты сервиса фоновой рассылки новостей. Чтение лент и отправка сообщений
 * подменяются фейками, время контролируется управляемыми часами.
 */
public class MailingServiceTest {

    /**
     * Идентификатор пользователя.
     */
    private static final long USER_ID = 1;

    /**
     * Ссылка на тестовую ленту.
     */
    private static final String FEED_URL = "https://habr.com/rss";

    /**
     * Начало тестового времени: полдень, не тихие часы.
     */
    private static final Instant START_TIME = Instant.parse("2024-06-01T12:00:00Z");

    /**
     * Хранилище данных пользователей.
     */
    private MemoryStorage storage;

    /**
     * Фейковое чтение лент.
     */
    private FakeReader reader;

    /**
     * Фейковая отправка сообщений.
     */
    private FakeSender sender;

    /**
     * Управляемые часы.
     */
    private MutableClock clock;

    /**
     * Проверяемый сервис рассылки.
     */
    private MailingService service;

    /**
     * Создаёт сервис с фейками и пользователем с одной лентой.
     */
    @BeforeEach
    void setUp() {
        storage = new MemoryStorage();
        storage.add(USER_ID, new Feed("Хабр", FEED_URL));
        storage.setMailingSettings(USER_ID, new MailingSettings(60, null, null));
        reader = new FakeReader();
        sender = new FakeSender();
        clock = new MutableClock(START_TIME);
        service = new MailingService(storage, reader, sender, clock);
    }

    /**
     * Тестирование первого запуска: текущие записи ленты помечаются как
     * отправленные, но не присылаются, а следующая новость приходит один раз.
     */
    @Test
    void testFirstRunMarksNewsWithoutSending() {
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                "https://habr.com/1", ""));
        service.checkOnce();
        Assertions.assertTrue(sender.messages.isEmpty());
        Assertions.assertTrue(storage.isNewsSent(USER_ID, "https://habr.com/1"));

        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                        "https://habr.com/1", ""),
                new FeedItem("Вторая новость", "https://habr.com/2", ""));
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        Assertions.assertEquals(List.of("Новость из ленты «Хабр»: "
                + "Вторая новость — https://habr.com/2"), sender.messages);
    }

    /**
     * Тестирование того, что новость приходит ровно один раз: повторная
     * проверка той же ленты ничего не шлёт.
     */
    @Test
    void testNewsSentOnlyOnce() {
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                "https://habr.com/1", ""));
        service.checkOnce();
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                        "https://habr.com/1", ""),
                new FeedItem("Вторая новость", "https://habr.com/2", ""));
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        Assertions.assertEquals(1, sender.messages.size());
    }

    /**
     * Тестирование интервала проверки: лента не проверяется чаще заданного
     * интервала.
     */
    @Test
    void testIntervalIsRespected() {
        MailingSettings settings = storage.getMailingSettings(USER_ID);
        storage.setMailingSettings(USER_ID, new MailingSettings(
                300, settings.quietFromHour(), settings.quietToHour()));
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                "https://habr.com/1", ""));
        service.checkOnce();
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                        "https://habr.com/1", ""),
                new FeedItem("Вторая новость", "https://habr.com/2", ""));
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        Assertions.assertTrue(sender.messages.isEmpty());
        clock.advance(Duration.ofMinutes(4));
        service.checkOnce();
        Assertions.assertEquals(1, sender.messages.size());
    }

    /**
     * Тестирование тихих часов: ночью новости не отправляются, после конца
     * паузы накопившиеся новости приходят ровно один раз.
     */
    @Test
    void testQuietHours() {
        storage.setMailingSettings(USER_ID, new MailingSettings(60, 23, 8));
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                "https://habr.com/1", ""));
        service.checkOnce();

        // 23:00 — тихие часы: новость не уходит и не помечается как отправленная.
        clock.advance(Duration.ofHours(11));
        reader.setNews(FEED_URL, new FeedItem("Первая новость",
                        "https://habr.com/1", ""),
                new FeedItem("Ночная новость", "https://habr.com/night", ""));
        service.checkOnce();
        Assertions.assertTrue(sender.messages.isEmpty());
        Assertions.assertFalse(storage.isNewsSent(USER_ID, "https://habr.com/night"));
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        Assertions.assertTrue(sender.messages.isEmpty());

        // 08:00 следующего дня — пауза закончилась: новость приходит один раз.
        clock.advance(Duration.ofHours(9));
        service.checkOnce();
        Assertions.assertEquals(List.of("Новость из ленты «Хабр»: "
                + "Ночная новость — https://habr.com/night"), sender.messages);
        clock.advance(Duration.ofMinutes(1));
        service.checkOnce();
        Assertions.assertEquals(1, sender.messages.size());
    }

    /**
     * Фейковое чтение лент: новости задаются по ссылке на ленту.
     */
    private static class FakeReader implements NewsReader {

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

    /**
     * Фейковая отправка сообщений: запоминает тексты сообщений.
     */
    private static class FakeSender implements NewsSender {

        /**
         * Отправленные сообщения.
         */
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendNews(long userId, String text) {
            messages.add(text);
        }
    }

    /**
     * Управляемые часы: время двигается вперёд только по команде.
     */
    private static class MutableClock extends Clock {

        /**
         * Текущий момент времени.
         */
        private Instant instant;

        /**
         * Создаёт часы, показывающие указанный момент.
         */
        MutableClock(Instant instant) {
            this.instant = instant;
        }

        /**
         * Сдвигает время вперёд.
         */
        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
