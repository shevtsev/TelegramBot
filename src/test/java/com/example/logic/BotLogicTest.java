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
         * Ссылка на вторую тестовую ленту.
         */
        private static final String OTHER_URL = "https://example.com/rss";

        /**
         * Название второй тестовой ленты.
         */
        private static final String OTHER_NAME = "Хабр 2";

        /**
         * Данные кнопки "Показать ещё".
         */
        private static final String MORE_DATA = "more";

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
                Assertions.assertEquals("Лента добавлена",
                                textAnswer(USER_ID, "/add " + FEED_URL));
                Assertions.assertEquals("1. " + FEED_URL,
                                textAnswer(USER_ID, "/list"));
                Assertions.assertEquals("Лента удалена",
                                textAnswer(USER_ID, "/remove " + FEED_URL));
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
                Assertions.assertEquals("Такой ленты нет в списке",
                                textAnswer(USER_ID, "/remove " + FEED_NAME));
        }

        /**
         * Тестирование удаления ленты по названию (без учёта регистра).
         */
        @Test
        void testRemoveByName() {
                storage.add(USER_ID, new Feed(FEED_NAME, FEED_URL));
                storage.add(USER_ID, new Feed(OTHER_NAME, OTHER_URL));
                Assertions.assertEquals("Лента удалена",
                                textAnswer(USER_ID, "/remove " + FEED_NAME.toLowerCase()));
                Assertions.assertEquals("1. " + OTHER_NAME + " — " + OTHER_URL,
                                textAnswer(USER_ID, "/list"));
                Assertions.assertEquals("Лента удалена",
                                textAnswer(USER_ID, "/remove " + OTHER_NAME));
                Assertions.assertEquals("Подписок пока нет. Добавь ленту командой /add <url>",
                                textAnswer(USER_ID, "/list"));
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
         * Тестирование пустого названия в диалоге: бот просит название ещё раз и
         * остаётся на том же шаге.
         */
        @Test
        void testAddDialogEmptyName() {
                botLogic.userAnswer(USER_ID, "/add");
                botLogic.userAnswer(USER_ID, FEED_URL);
                Assertions.assertEquals("Название не может быть пустым. Как назвать ленту?",
                                textAnswer(USER_ID, "   "));
                Assertions.assertEquals("Лента “" + FEED_NAME + "” добавлена",
                                textAnswer(USER_ID, FEED_NAME));
        }

        /**
         * Тестирование повторного названия в диалоге: название, которое уже есть у
         * другой ленты (в любом регистре), не принимается, и бот ждёт другое.
         */
        @Test
        void testAddDialogRepeatedName() {
                storage.add(USER_ID, new Feed(FEED_NAME, OTHER_URL));
                botLogic.userAnswer(USER_ID, "/add");
                botLogic.userAnswer(USER_ID, FEED_URL);
                String error = "Лента с таким названием уже есть. Пришлите другое название или /cancel";
                Assertions.assertEquals(error, textAnswer(USER_ID, FEED_NAME));
                Assertions.assertEquals(error, textAnswer(USER_ID, FEED_NAME.toLowerCase()));
                Assertions.assertEquals("Лента “" + OTHER_NAME + "” добавлена",
                                textAnswer(USER_ID, OTHER_NAME));
                Assertions.assertEquals("1. " + FEED_NAME + " — " + OTHER_URL
                                + "\n2. " + OTHER_NAME + " — " + FEED_URL, textAnswer(USER_ID, "/list"));
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
         * Тестирование просмотра новостей ленты по её названию (без учёта
         * регистра) и перехода на следующую страницу.
         */
        @Test
        void testLatestByName() {
                storage.add(USER_ID, new Feed(FEED_NAME, FEED_URL));
                Answer first = botLogic.userAnswer(USER_ID, "/latest " + FEED_NAME.toLowerCase());
                Assertions.assertEquals("""
                                1. Первая — https://example.com/1
                                2. Вторая — https://example.com/2
                                3. Третья — https://example.com/3""", first.text());
                Assertions.assertEquals(List.of(new Button("Показать ещё", MORE_DATA)),
                                first.buttons());
                Answer second = botLogic.callbackAnswer(USER_ID, MORE_DATA);
                Assertions.assertEquals("""
                                4. Четвёртая — https://example.com/4
                                5. Пятая — https://example.com/5""", second.text());
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
                Assertions.assertEquals("Не удалось прочитать ленту",
                                textAnswer(USER_ID, "/latest " + FEED_NAME));
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

        /**
         * Тестирование настроек рассылки по умолчанию: проверка лент каждые
         * 30 минут, тихие часы не настроены.
         */
        @Test
        void testDefaultSettings() {
                Assertions.assertEquals(
                                "Проверка лент: каждые 30 минут. Тихие часы: не настроены",
                                textAnswer(USER_ID, "/settings"));
        }

        /**
         * Тестирование установки интервала параметром: число в минутах и
         * интервал с единицей времени (s, m, h, d).
         */
        @Test
        void testIntervalWithParameter() {
                Assertions.assertEquals("Буду проверять ленты каждые 30 минут",
                                textAnswer(USER_ID, "/interval 30"));
                Assertions.assertEquals("Буду проверять ленты каждые 15 минут",
                                textAnswer(USER_ID, "/interval 15m"));
                Assertions.assertEquals("Буду проверять ленты каждые 2 часа",
                                textAnswer(USER_ID, "/interval 2h"));
                Assertions.assertEquals("Буду проверять ленты каждые 30 секунд",
                                textAnswer(USER_ID, "/interval 30s"));
                Assertions.assertEquals("Буду проверять ленты каждый день",
                                textAnswer(USER_ID, "/interval 1d"));
        }

        /**
         * Тестирование ошибок команды /interval.
         */
        @Test
        void testIntervalErrors() {
                String error = "Интервал должен быть от 1 секунды до 7 дней, например: 30, 15m, 2h, 1d";
                Assertions.assertEquals(error, textAnswer(USER_ID, "/interval abc"));
                Assertions.assertEquals(error, textAnswer(USER_ID, "/interval 0"));
                Assertions.assertEquals(error, textAnswer(USER_ID, "/interval 8d"));
                Assertions.assertEquals(error, textAnswer(USER_ID, "/interval -5"));
        }

        /**
         * Тестирование пошаговой установки интервала: бот спрашивает интервал
         * и сохраняет присланное значение.
         */
        @Test
        void testIntervalDialog() {
                Assertions.assertEquals(
                                "Как часто проверять ленты? Пришлите интервал, например: 30, 15m, 2h, 1d",
                                textAnswer(USER_ID, "/interval"));
                Assertions.assertEquals("Буду проверять ленты каждые 45 минут",
                                textAnswer(USER_ID, "45m"));
                Assertions.assertEquals(
                                "Проверка лент: каждые 45 минут. Тихие часы: не настроены",
                                textAnswer(USER_ID, "/settings"));
        }

        /**
         * Тестирование пошаговой настройки тихих часов: бот спрашивает час
         * начала, затем час конца.
         */
        @Test
        void testQuietDialog() {
                Assertions.assertEquals(
                                "С какого часа не присылать новости? Пришлите число от 0 до 23",
                                textAnswer(USER_ID, "/quiet"));
                Assertions.assertEquals("До какого часа?", textAnswer(USER_ID, "23"));
                Assertions.assertEquals("Не буду присылать новости с 23:00 до 08:00",
                                textAnswer(USER_ID, "8"));
                Assertions.assertEquals(
                                "Проверка лент: каждые 30 минут. Тихие часы: с 23:00 до 08:00",
                                textAnswer(USER_ID, "/settings"));
        }

        /**
         * Тестирование установки тихих часов параметрами и их отключения
         * командой /quiet off.
         */
        @Test
        void testQuietWithParametersAndOff() {
                Assertions.assertEquals("Не буду присылать новости с 22:00 до 07:00",
                                textAnswer(USER_ID, "/quiet 22 7"));
                Assertions.assertEquals("Тихие часы отключены",
                                textAnswer(USER_ID, "/quiet off"));
                Assertions.assertEquals(
                                "Проверка лент: каждые 30 минут. Тихие часы: не настроены",
                                textAnswer(USER_ID, "/settings"));
        }

        /**
         * Тестирование ошибок в диалоге тихих часов: бот остаётся на том же
         * шаге, а /cancel отменяет настройку.
         */
        @Test
        void testQuietErrorsAndCancel() {
                String hourError = "Пришлите час от 0 до 23 или /cancel";
                botLogic.userAnswer(USER_ID, "/quiet");
                Assertions.assertEquals(hourError, textAnswer(USER_ID, "abc"));
                Assertions.assertEquals(hourError, textAnswer(USER_ID, "24"));
                Assertions.assertEquals("До какого часа?", textAnswer(USER_ID, "23"));
                Assertions.assertEquals(hourError, textAnswer(USER_ID, "-1"));
                Assertions.assertEquals("Настройка отменена", textAnswer(USER_ID, "/cancel"));
                Assertions.assertEquals(
                                "Проверка лент: каждые 30 минут. Тихие часы: не настроены",
                                textAnswer(USER_ID, "/settings"));
        }

        /**
         * Тестирование ошибки команды /quiet с одним параметром.
         */
        @Test
        void testQuietOneParameterError() {
                Assertions.assertEquals(
                                "Укажите тихие часы двумя числами от 0 до 23: /quiet <с> <до>",
                                textAnswer(USER_ID, "/quiet 23"));
        }

        /**
         * Тестирование того, что настройки рассылки свои у каждого
         * пользователя, а установка тихих часов не сбрасывает интервал.
         */
        @Test
        void testSettingsArePerUser() {
                botLogic.userAnswer(USER_ID, "/interval 5m");
                botLogic.userAnswer(USER_ID, "/quiet 23 8");
                Assertions.assertEquals(
                                "Проверка лент: каждые 30 минут. Тихие часы: не настроены",
                                textAnswer(OTHER_USER_ID, "/settings"));
                Assertions.assertEquals(
                                "Проверка лент: каждые 5 минут. Тихие часы: с 23:00 до 08:00",
                                textAnswer(USER_ID, "/settings"));
        }
}
