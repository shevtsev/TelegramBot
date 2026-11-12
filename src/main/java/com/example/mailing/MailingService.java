package com.example.mailing;

import com.example.feed.FeedItem;
import com.example.feed.NewsReader;
import com.example.storage.Feed;
import com.example.storage.MailingSettings;
import com.example.storage.Storage;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис фоновой рассылки новостей: раз в минуту обходит пользователей с
 * лентами, читает ленты с учётом интервала проверки и тихих часов и шлёт
 * новые новости. Каждая новость отправляется только один раз: при первой
 * проверке текущие записи ленты только помечаются как отправленные, чтобы
 * не присылать старые новости.
 */
public class MailingService implements AutoCloseable {

    /**
     * Период обхода пользователей в секундах.
     */
    private static final long TICK_SECONDS = 60;

    /**
     * Хранилище данных пользователей.
     */
    private final Storage storage;

    /**
     * Чтение новостей из лент.
     */
    private final NewsReader reader;

    /**
     * Отправка новостей пользователям.
     */
    private final NewsSender sender;

    /**
     * Часы для определения текущего времени.
     */
    private final Clock clock;

    /**
     * Планировщик фоновой рассылки.
     */
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * Время последней проверки лент по каждому пользователю.
     */
    private final Map<Long, Instant> lastChecks = new HashMap<>();

    /**
     * Создаёт сервис рассылки с системными часами.
     */
    public MailingService(Storage storage, NewsReader reader, NewsSender sender) {
        this(storage, reader, sender, Clock.systemDefaultZone());
    }

    /**
     * Создаёт сервис рассылки с указанными часами.
     */
    public MailingService(Storage storage, NewsReader reader, NewsSender sender,
            Clock clock) {
        this.storage = storage;
        this.reader = reader;
        this.sender = sender;
        this.clock = clock;
    }

    /**
     * Запускает фоновую рассылку: обход пользователей раз в минуту.
     */
    public void start() {
        executor.scheduleWithFixedDelay(this::safeCheck, 0, TICK_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * Останавливает фоновую рассылку.
     */
    @Override
    public void close() {
        executor.shutdownNow();
    }

    /**
     * Выполняет один обход всех пользователей с лентами.
     */
    void checkOnce() {
        for (long userId : storage.getUserIds()) {
            checkUser(userId);
        }
    }

    /**
     * Проверяет ленты одного пользователя и шлёт новые новости, если пришло
     * время проверки и сейчас не тихие часы.
     */
    private void checkUser(long userId) {
        MailingSettings settings = storage.getMailingSettings(userId);
        ZonedDateTime now = ZonedDateTime.now(clock);
        if (isQuietHour(settings, now.getHour())) {
            return;
        }
        Instant lastCheck = lastChecks.get(userId);
        if (lastCheck != null
                && Duration.between(lastCheck, now.toInstant()).getSeconds()
                        < settings.checkIntervalSeconds()) {
            return;
        }
        boolean firstRun = !storage.hasSentNews(userId);
        boolean checked = false;
        for (Feed feed : storage.getAll(userId)) {
            List<FeedItem> news;
            try {
                news = reader.read(feed.url());
            } catch (IOException e) {
                System.err.println("Не удалось прочитать ленту " + feed.url() + ": "
                        + e.getMessage());
                continue;
            }
            checked = true;
            for (FeedItem item : news) {
                if (!storage.addSentNews(userId, item.getLink())) {
                    continue;
                }
                if (!firstRun) {
                    sender.sendNews(userId, formatNews(feed, item));
                }
            }
        }
        if (checked) {
            lastChecks.put(userId, now.toInstant());
        }
    }

    /**
     * Выполняет обход, не давая ошибкам остановить планировщик.
     */
    private void safeCheck() {
        try {
            checkOnce();
        } catch (RuntimeException e) {
            System.err.println("Ошибка при рассылке новостей: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Возвращает true, если указанный час попадает в тихие часы. Период может
     * пересекать полночь, например с 23 до 8.
     */
    private boolean isQuietHour(MailingSettings settings, int hour) {
        if (settings.quietFromHour() == null || settings.quietToHour() == null) {
            return false;
        }
        int from = settings.quietFromHour();
        int to = settings.quietToHour();
        return from <= to ? hour >= from && hour < to : hour >= from || hour < to;
    }

    /**
     * Возвращает текст сообщения о новой новости ленты.
     */
    private String formatNews(Feed feed, FeedItem item) {
        String source = feed.name().isEmpty()
                ? feed.url()
                : "«" + feed.name() + "»";
        return "Новость из ленты " + source + ": "
                + item.getTitle() + " — " + item.getLink();
    }
}
