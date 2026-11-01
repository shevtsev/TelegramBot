package com.example.logic;

import com.example.feed.FeedItem;
import com.example.storage.Feed;
import com.example.storage.MemoryStorage;
import com.example.storage.Storage;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

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
     * Ссылка на ленту, которую не удаётся прочитать.
     */
    private static final String BROKEN_URL = "https://broken.com/rss";

    /**
     * Ссылка на ленту без новостей.
     */
    private static final String EMPTY_URL = "https://empty.com/rss";

    /**
     * Название тестовой ленты.
     */
    private static final String FEED_NAME = "Хабр";

    /**
     * Данные кнопки "Показать ещё".
     */
    private static final String MORE_DATA = "more";

    /**
     * Новости тестовой ленты.
     */
    private static final List<FeedItem> NEWS = List.of(
            new FeedItem("Первая", "https://example.com/1", ""),
            new FeedItem("Вторая", "https://example.com/2", ""),
            new FeedItem("Третья", "https://example.com/3", ""),
            new FeedItem("Четвёртая", "https://example.com/4", ""),
            new FeedItem("Пятая", "https://example.com/5", ""));

    /**
     * Хранилище данных пользователей для тестов.
     */
    private Storage storage;

    /**
     * Логика бота с тестовым хранилищем и тестовой лентой: ссылки не на
     * https и BROKEN_URL прочитать нельзя.
     */
    private BotLogic botLogic;

    /**
     * Создаёт пустое хранилище и новую логику бота перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        storage = new MemoryStorage();
        botLogic = new BotLogic(storage, url -> {
            if (!url.startsWith("https://") || url.equals(BROKEN_URL)) {
                throw new IOException("Лента недоступна");
            }
            return url.equals(EMPTY_URL) ? List.of() : NEWS;
        });
    }

    /**
     * Возвращает текст ответа бота на сообщение пользователя.
     */
    private String textAnswer(long userId, String message) {
        return botLogic.userAnswer(userId, message).text();
    }

    /**
     * Тестирование добавления, просмотра и удаления ленты.
     */
    @Test
    void testAddListRemove() {
        Assertions.assertEquals("Лента добавлена", textAnswer(USER_ID, "/add " + FEED_URL));
        Assertions.assertEquals("1. " + FEED_URL, textAnswer(USER_ID, "/list"));
        Assertions.assertEquals("Лента удалена", textAnswer(USER_ID, "/remove " + FEED_URL));
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что ленту, которую не удаётся прочитать, нельзя
     * добавить.
     */
    @Test
    void testAddFeedThatCannotBeRead() {
        Assertions.assertEquals("Не удалось найти рассылку",
                textAnswer(USER_ID, "/add " + BROKEN_URL));
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование показа ленты с названием в списке лент.
     */
    @Test
    void testListShowsFeedName() {
        storage.add(USER_ID, new Feed("Хабр", FEED_URL));
        Assertions.assertEquals("1. Хабр — " + FEED_URL, textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что у разных пользователей разные списки лент.
     */
    @Test
    void testUsersHaveSeparateSubscriptions() {
        botLogic.userAnswer(USER_ID, "/add " + FEED_URL);
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                textAnswer(OTHER_USER_ID, "/list"));
        Assertions.assertEquals("Такой ленты нет в списке",
                textAnswer(OTHER_USER_ID, "/remove " + FEED_URL));
        Assertions.assertEquals("1. " + FEED_URL, textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование ошибок при добавлении ленты.
     */
    @Test
    void testAddErrors() {
        Assertions.assertEquals("Не удалось найти рассылку", textAnswer(USER_ID, "/add habr"));
        botLogic.userAnswer(USER_ID, "/add " + FEED_URL);
        Assertions.assertEquals("Эта лента уже есть в списке",
                textAnswer(USER_ID, "/add " + FEED_URL));
        Assertions.assertEquals("1. " + FEED_URL, textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что удаляется только выбранная лента.
     */
    @Test
    void testRemoveOnlyChosenFeed() {
        String otherUrl = "https://example.com/rss";
        botLogic.userAnswer(USER_ID, "/add " + FEED_URL);
        botLogic.userAnswer(USER_ID, "/add " + otherUrl);
        Assertions.assertEquals("Лента удалена", textAnswer(USER_ID, "/remove " + FEED_URL));
        Assertions.assertEquals("1. " + otherUrl, textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование ошибок при удалении ленты.
     */
    @Test
    void testRemoveErrors() {
        Assertions.assertEquals("Укажите ссылку: /remove <url>",
                textAnswer(USER_ID, "/remove"));
        Assertions.assertEquals("Такой ленты нет в списке",
                textAnswer(USER_ID, "/remove " + FEED_URL));
    }

    /**
     * Тестирование обычного диалога добавления ленты: ссылка, затем название.
     */
    @Test
    void testAddDialog() {
        Assertions.assertEquals("Пришлите ссылку на ленту", textAnswer(USER_ID, "/add"));
        Assertions.assertEquals("Лента найдена. Как её назвать?",
                textAnswer(USER_ID, FEED_URL));
        Assertions.assertEquals("Лента “" + FEED_NAME + "” добавлена",
                textAnswer(USER_ID, FEED_NAME));
        Assertions.assertEquals("1. " + FEED_NAME + " — " + FEED_URL,
                textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование ошибки в диалоге: при неверной ссылке бот остаётся на шаге
     * ссылки, а /cancel отменяет добавление.
     */
    @Test
    void testAddDialogErrorAndCancel() {
        botLogic.userAnswer(USER_ID, "/add");
        Assertions.assertEquals("Не удалось найти рассылку. Пришлите ссылку ещё раз или /cancel",
                textAnswer(USER_ID, "habr"));
        Assertions.assertEquals("Добавление отменено", textAnswer(USER_ID, "/cancel"));
        Assertions.assertEquals("Неизвестная команда. Список команд: /help",
                textAnswer(USER_ID, FEED_URL));
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                textAnswer(USER_ID, "/list"));
    }

    /**
     * Тестирование того, что после неверной ссылки диалог продолжается с того
     * же шага.
     */
    @Test
    void testAddDialogRetryUrl() {
        botLogic.userAnswer(USER_ID, "/add");
        botLogic.userAnswer(USER_ID, BROKEN_URL);
        Assertions.assertEquals("Лента найдена. Как её назвать?",
                textAnswer(USER_ID, FEED_URL));
    }

    /**
     * Тестирование того, что ленту, которая уже есть в списке, нельзя добавить
     * через диалог, и диалог после этого завершается.
     */
    @Test
    void testAddDialogDuplicate() {
        botLogic.userAnswer(USER_ID, "/add " + FEED_URL);
        botLogic.userAnswer(USER_ID, "/add");
        Assertions.assertEquals("Эта лента уже есть в списке", textAnswer(USER_ID, FEED_URL));
        Assertions.assertEquals("Нечего отменять", textAnswer(USER_ID, "/cancel"));
    }

    /**
     * Тестирование двух пользователей на разных шагах диалога: один ждёт
     * название, другой ещё ждёт ссылку.
     */
    @Test
    void testAddDialogUsersOnDifferentSteps() {
        botLogic.userAnswer(USER_ID, "/add");
        botLogic.userAnswer(OTHER_USER_ID, "/add");
        botLogic.userAnswer(USER_ID, FEED_URL);
        Assertions.assertEquals("Не удалось найти рассылку. Пришлите ссылку ещё раз или /cancel",
                textAnswer(OTHER_USER_ID, FEED_NAME));
        Assertions.assertEquals("Лента “" + FEED_NAME + "” добавлена",
                textAnswer(USER_ID, FEED_NAME));
        Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                textAnswer(OTHER_USER_ID, "/list"));
    }

    /**
     * Тестирование команды /cancel без начатого диалога.
     */
    @Test
    void testCancelWithoutDialog() {
        Assertions.assertEquals("Нечего отменять", textAnswer(USER_ID, "/cancel"));
    }

    /**
     * Тестирование первой страницы новостей: по умолчанию 3 новости и кнопка
     * "Показать ещё".
     */
    @Test
    void testLatestFirstPage() {
        Answer answer = botLogic.userAnswer(USER_ID, "/latest " + FEED_URL);
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2
                3. Третья — https://example.com/3""", answer.text());
        Assertions.assertEquals(List.of(new Button("Показать ещё", MORE_DATA)),
                answer.buttons());
    }

    /**
     * Тестирование следующих страниц: после последней страницы кнопки нет.
     */
    @Test
    void testShowMore() {
        botLogic.userAnswer(USER_ID, "/latest " + FEED_URL);
        Answer second = botLogic.callbackAnswer(USER_ID, MORE_DATA);
        Assertions.assertEquals("""
                4. Четвёртая — https://example.com/4
                5. Пятая — https://example.com/5""", second.text());
        Assertions.assertTrue(second.buttons().isEmpty());
        Assertions.assertEquals("Больше новостей нет",
                botLogic.callbackAnswer(USER_ID, MORE_DATA).text());
    }

    /**
     * Тестирование кнопки "Показать ещё" без выбранной ленты.
     */
    @Test
    void testShowMoreWithoutLatest() {
        Assertions.assertEquals("Сначала выберите ленту: /latest <url>",
                botLogic.callbackAnswer(USER_ID, MORE_DATA).text());
    }

    /**
     * Тестирование ошибок команды /latest.
     */
    @Test
    void testLatestErrors() {
        Assertions.assertEquals("Укажите ссылку: /latest <url>",
                textAnswer(USER_ID, "/latest"));
        Assertions.assertEquals("Не удалось прочитать ленту",
                textAnswer(USER_ID, "/latest " + BROKEN_URL));
        Assertions.assertEquals("В ленте нет новостей",
                textAnswer(USER_ID, "/latest " + EMPTY_URL));
    }

    /**
     * Тестирование размера страницы.
     */
    @Test
    void testPageSize() {
        Assertions.assertEquals("Размер страницы изменён: 2",
                textAnswer(USER_ID, "/pagesize 2"));
        Answer answer = botLogic.userAnswer(USER_ID, "/latest " + FEED_URL);
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2""", answer.text());
        Assertions.assertFalse(answer.buttons().isEmpty());
    }

    /**
     * Тестирование того, что размер страницы у каждого пользователя свой.
     */
    @Test
    void testPageSizeIsPerUser() {
        botLogic.userAnswer(USER_ID, "/pagesize 1");
        Assertions.assertEquals("""
                1. Первая — https://example.com/1
                2. Вторая — https://example.com/2
                3. Третья — https://example.com/3""",
                textAnswer(OTHER_USER_ID, "/latest " + FEED_URL));
    }

    /**
     * Тестирование ошибок команды /pagesize.
     */
    @Test
    void testPageSizeErrors() {
        String error = "Размер страницы должен быть числом от 1 до 20";
        Assertions.assertEquals(error, textAnswer(USER_ID, "/pagesize"));
        Assertions.assertEquals(error, textAnswer(USER_ID, "/pagesize abc"));
        Assertions.assertEquals(error, textAnswer(USER_ID, "/pagesize 0"));
        Assertions.assertEquals(error, textAnswer(USER_ID, "/pagesize 21"));
    }
}
