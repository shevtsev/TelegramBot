package com.example.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранилище данных пользователей в базе данных (PostgreSQL) через JDBC.
 */
public class DbStorage implements Storage, AutoCloseable {

    /**
     * Создание таблицы с данными пользователей.
     */
    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                user_id BIGINT PRIMARY KEY,
                page_size INT NOT NULL,
                position_url TEXT,
                position_shown INT,
                dialog_step VARCHAR(20) NOT NULL,
                dialog_url TEXT NOT NULL,
                check_interval_minutes INT NOT NULL,
                quiet_from INT,
                quiet_to INT)""";

    /**
     * Создание таблицы с лентами пользователей.
     */
    private static final String CREATE_FEEDS = """
            CREATE TABLE IF NOT EXISTS feeds (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                user_id BIGINT NOT NULL,
                url VARCHAR(2048) NOT NULL,
                name TEXT NOT NULL,
                UNIQUE (user_id, url))""";

    /**
     * Создание таблицы с уже отправленными новостями.
     */
    private static final String CREATE_SENT_NEWS = """
            CREATE TABLE IF NOT EXISTS sent_news (
                user_id BIGINT NOT NULL,
                news_url VARCHAR(2048) NOT NULL,
                PRIMARY KEY (user_id, news_url))""";

    /**
     * Создание пользователя с настройками по умолчанию, если его ещё нет.
     */
    private static final String INSERT_USER = """
            INSERT INTO users (user_id, page_size, dialog_step, dialog_url,
                check_interval_minutes)
            VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING""";

    /**
     * Добавление ленты, если ленты с такой ссылкой у пользователя ещё нет.
     */
    private static final String INSERT_FEED = """
            INSERT INTO feeds (user_id, url, name) VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING""";

    /**
     * Удаление ленты пользователя по ссылке.
     */
    private static final String DELETE_FEED =
            "DELETE FROM feeds WHERE user_id = ? AND url = ?";

    /**
     * Получение лент пользователя в порядке добавления.
     */
    private static final String SELECT_FEEDS =
            "SELECT name, url FROM feeds WHERE user_id = ? ORDER BY id";

    /**
     * Получение размера страницы новостей.
     */
    private static final String SELECT_PAGE_SIZE =
            "SELECT page_size FROM users WHERE user_id = ?";

    /**
     * Изменение размера страницы новостей.
     */
    private static final String UPDATE_PAGE_SIZE =
            "UPDATE users SET page_size = ? WHERE user_id = ?";

    /**
     * Получение позиции в ленте.
     */
    private static final String SELECT_POSITION =
            "SELECT position_url, position_shown FROM users WHERE user_id = ?";

    /**
     * Изменение позиции в ленте.
     */
    private static final String UPDATE_POSITION =
            "UPDATE users SET position_url = ?, position_shown = ? WHERE user_id = ?";

    /**
     * Получение состояния диалога добавления ленты.
     */
    private static final String SELECT_DIALOG =
            "SELECT dialog_step, dialog_url FROM users WHERE user_id = ?";

    /**
     * Изменение состояния диалога добавления ленты.
     */
    private static final String UPDATE_DIALOG =
            "UPDATE users SET dialog_step = ?, dialog_url = ? WHERE user_id = ?";

    /**
     * Получение настроек рассылки новостей.
     */
    private static final String SELECT_MAILING_SETTINGS = """
            SELECT check_interval_minutes, quiet_from, quiet_to
            FROM users WHERE user_id = ?""";

    /**
     * Изменение настроек рассылки новостей.
     */
    private static final String UPDATE_MAILING_SETTINGS = """
            UPDATE users SET check_interval_minutes = ?, quiet_from = ?, quiet_to = ?
            WHERE user_id = ?""";

    /**
     * Получение идентификаторов пользователей, у которых есть ленты.
     */
    private static final String SELECT_USER_IDS =
            "SELECT DISTINCT user_id FROM feeds";

    /**
     * Пометка новости как отправленной, если она ещё не отмечена.
     */
    private static final String INSERT_SENT_NEWS = """
            INSERT INTO sent_news (user_id, news_url) VALUES (?, ?)
            ON CONFLICT DO NOTHING""";

    /**
     * Проверка, что новость уже была отправлена пользователю.
     */
    private static final String SELECT_SENT_NEWS = """
            SELECT 1 FROM sent_news WHERE user_id = ? AND news_url = ?""";

    /**
     * Соединение с базой данных.
     */
    private final Connection connection;

    /**
     * Подключается к базе данных и создаёт таблицы, если их ещё нет.
     */
    public DbStorage(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new StorageException("Не удалось подключиться к базе данных", e);
        }
        update(CREATE_USERS);
        update(CREATE_FEEDS);
        update(CREATE_SENT_NEWS);
    }

    @Override
    public boolean add(long userId, Feed feed) {
        return update(INSERT_FEED, userId, feed.url(), feed.name()) > 0;
    }

    @Override
    public boolean remove(long userId, String url) {
        return update(DELETE_FEED, userId, url) > 0;
    }

    @Override
    public List<Feed> getAll(long userId) {
        try (PreparedStatement statement = prepare(SELECT_FEEDS, userId);
                ResultSet result = statement.executeQuery()) {
            List<Feed> feeds = new ArrayList<>();
            while (result.next()) {
                feeds.add(new Feed(result.getString("name"), result.getString("url")));
            }
            return feeds;
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public int getPageSize(long userId) {
        try (PreparedStatement statement = prepare(SELECT_PAGE_SIZE, userId);
                ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt("page_size") : DEFAULT_PAGE_SIZE;
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public void setPageSize(long userId, int size) {
        createUserIfMissing(userId);
        update(UPDATE_PAGE_SIZE, size, userId);
    }

    @Override
    public FeedPosition getFeedPosition(long userId) {
        try (PreparedStatement statement = prepare(SELECT_POSITION, userId);
                ResultSet result = statement.executeQuery()) {
            if (!result.next() || result.getString("position_url") == null) {
                return null;
            }
            return new FeedPosition(
                    result.getString("position_url"), result.getInt("position_shown"));
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public void setFeedPosition(long userId, FeedPosition position) {
        createUserIfMissing(userId);
        update(UPDATE_POSITION, position.url(), position.shownCount(), userId);
    }

    @Override
    public DialogState getDialogState(long userId) {
        try (PreparedStatement statement = prepare(SELECT_DIALOG, userId);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return DialogState.NO_DIALOG;
            }
            return new DialogState(
                    DialogStep.valueOf(result.getString("dialog_step")),
                    result.getString("dialog_url"));
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public void setDialogState(long userId, DialogState state) {
        createUserIfMissing(userId);
        update(UPDATE_DIALOG, state.step().name(), state.url(), userId);
    }

    @Override
    public MailingSettings getMailingSettings(long userId) {
        try (PreparedStatement statement = prepare(SELECT_MAILING_SETTINGS, userId);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return MailingSettings.DEFAULT;
            }
            int checkInterval = result.getInt("check_interval_minutes");
            int quietFrom = result.getInt("quiet_from");
            boolean quietFromMissing = result.wasNull();
            int quietTo = result.getInt("quiet_to");
            return new MailingSettings(
                    checkInterval,
                    quietFromMissing ? null : quietFrom,
                    result.wasNull() ? null : quietTo);
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public void setMailingSettings(long userId, MailingSettings settings) {
        createUserIfMissing(userId);
        update(UPDATE_MAILING_SETTINGS, settings.checkIntervalMinutes(),
                settings.quietFromHour(), settings.quietToHour(), userId);
    }

    @Override
    public List<Long> getUserIds() {
        try (PreparedStatement statement = prepare(SELECT_USER_IDS);
                ResultSet result = statement.executeQuery()) {
            List<Long> userIds = new ArrayList<>();
            while (result.next()) {
                userIds.add(result.getLong("user_id"));
            }
            return userIds;
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public boolean addSentNews(long userId, String newsUrl) {
        return update(INSERT_SENT_NEWS, userId, newsUrl) > 0;
    }

    @Override
    public boolean isNewsSent(long userId, String newsUrl) {
        try (PreparedStatement statement = prepare(SELECT_SENT_NEWS, userId, newsUrl);
                ResultSet result = statement.executeQuery()) {
            return result.next();
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new StorageException("Не удалось закрыть соединение с базой данных", e);
        }
    }

    /**
     * Создаёт пользователя с настройками по умолчанию, если его ещё нет в
     * базе.
     */
    private void createUserIfMissing(long userId) {
        update(INSERT_USER, userId, DEFAULT_PAGE_SIZE, DialogStep.NONE.name(),
                DialogState.NO_DIALOG.url(), MailingSettings.DEFAULT.checkIntervalMinutes());
    }

    /**
     * Выполняет запрос на изменение данных и возвращает число изменённых
     * строк.
     */
    private int update(String sql, Object... parameters) {
        try (PreparedStatement statement = prepare(sql, parameters)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw queryError(e);
        }
    }

    /**
     * Готовит запрос и подставляет в него параметры по порядку.
     */
    private PreparedStatement prepare(String sql, Object... parameters)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    /**
     * Создаёт ошибку хранилища для неудавшегося запроса к базе данных.
     */
    private StorageException queryError(SQLException cause) {
        return new StorageException(
                "Не удалось выполнить запрос к базе данных: " + cause.getMessage(), cause);
    }
}
