package com.example.logic;

import com.example.feed.NewsReader;
import com.example.storage.DialogState;
import com.example.storage.DialogStep;
import com.example.storage.Feed;
import com.example.storage.FeedPosition;
import com.example.storage.MailingSettings;
import com.example.storage.Storage;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс логики бота: разбирает команды и делегирует обработку диалогов
 * и операции с лентами обработчику состояний.
 */
public class BotLogic {

    /**
     * Ответ на команду /start.
     */
    private static final String START_MESSAGE = "Этот бот собирает новости из RSS-лент. Список команд: /help";

    /**
     * Ответ на команду /help.
     */
    private static final String HELP_MESSAGE = """
            Доступные команды:
            /start - начать
            /help - список команд
            /add - добавить ленту по шагам
            /add <url> - добавить ленту сразу
            /cancel - отменить текущий диалог
            /remove - удалить ленту по шагам
            /remove <название или url> - удалить ленту сразу
            /list - список лент
            /latest - показать новости по шагам
            /latest <название или url> - последние новости ленты
            /pagesize - задать размер страницы по шагам
            /pagesize <n> - сколько новостей показывать за раз
            /interval - задать интервал проверки по шагам
            /interval <время> - как часто проверять ленты (например: 30, 15m, 2h, 1d)
            /quiet - задать тихие часы по шагам
            /quiet <с> <до> - задать тихие часы сразу
            /quiet off - отключить тихие часы
            /settings - текущие настройки рассылки
            /search - искать новости по шагам
            /search <название или all> <слова> - найти новости по ключевым словам""";

    /**
     * Ответ на неизвестную команду.
     */
    private static final String UNKNOWN_MESSAGE = "Неизвестная команда. Список команд: /help";

    /**
     * Ответ, когда по ссылке не удалось прочитать ленту.
     */
    private static final String NOT_FOUND_MESSAGE = "Не удалось найти рассылку";

    /**
     * Просьба прислать ссылку в диалоге добавления ленты.
     */
    private static final String ASK_URL_MESSAGE = "Пришлите ссылку на ленту";

    /**
     * Просьба прислать интервал проверки лент.
     */
    private static final String ASK_INTERVAL_MESSAGE =
            "Как часто проверять ленты? Пришлите интервал, например: 30, 15m, 2h, 1d";

    /**
     * Просьба прислать название ленты для показа новостей.
     */
    private static final String ASK_LATEST_MESSAGE = "Новости какой ленты показать?";

    /**
     * Просьба прислать название ленты для удаления.
     */
    private static final String ASK_REMOVE_MESSAGE = "Какую ленту удалить?";

    /**
     * Просьба прислать размер страницы новостей.
     */
    private static final String ASK_PAGE_SIZE_MESSAGE =
            "Сколько новостей показывать за раз? Пришлите число от 1 до "
                    + DialogStateHandler.MAX_PAGE_SIZE;

    /**
     * Просьба прислать, где искать новости: название ленты или all.
     */
    private static final String ASK_SEARCH_FEED_MESSAGE =
            "Где искать? Пришлите название ленты или all, чтобы искать по всем подпискам";

    /**
     * Просьба прислать час начала тихих часов.
     */
    private static final String ASK_QUIET_FROM_MESSAGE =
            "С какого часа не присылать новости? Пришлите число от 0 до 23";

    /**
     * Хранилище данных пользователей.
     */
    private final Storage storage;

    /**
     * Обработчик шагов диалогов и операций с лентами.
     */
    private final DialogStateHandler dialogStateHandler;

    /**
     * Создаёт логику бота с хранилищем данных и чтением новостей из лент.
     */
    public BotLogic(Storage storage, NewsReader reader) {
        this.storage = storage;
        this.dialogStateHandler = new DialogStateHandler(storage, reader);
    }

    /**
     * Возвращает ответ на сообщение пользователя, либо вызывает команду, если
     * пользователь написал команду. Обычный текст считается ответом на шаг
     * диалога добавления ленты.
     */
    public Answer userAnswer(long userId, String text) {
        if (!text.startsWith("/")) {
            return dialogAnswer(userId, text.trim());
        }
        String[] parts = text.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        Answer answer = switch (parts[0]) {
            case "/start" -> new Answer(START_MESSAGE);
            case "/help" -> new Answer(HELP_MESSAGE);
            case "/add" -> new Answer(addFeed(userId, argument));
            case "/cancel" -> new Answer(cancelAnswer(userId));
            case "/remove" -> new Answer(removeFeed(userId, argument));
            case "/list" -> new Answer(listFeeds(userId));
            case "/latest" -> latest(userId, argument);
            case "/pagesize" -> new Answer(pageSizeAnswer(userId, argument));
            case "/interval" -> new Answer(intervalAnswer(userId, argument));
            case "/quiet" -> new Answer(quietAnswer(userId, argument));
            case "/settings" -> new Answer(settingsAnswer(userId));
            case "/search" -> search(userId, argument);
            default -> new Answer(UNKNOWN_MESSAGE);
        };
        return answer;
    }

    /**
     * Возвращает ответ на нажатие кнопки по её данным.
     */
    public Answer callbackAnswer(long userId, String data) {
        if (DialogStateHandler.MORE_DATA.equals(data)) {
            return moreAnswer(userId);
        }
        if (data != null && data.startsWith(DialogStateHandler.SEARCH_MORE_PREFIX)) {
            return dialogStateHandler.showMoreSearchResults(userId, data);
        }
        return new Answer("");
    }

    /**
     * Возвращает следующую страницу новостей, когда пользователь нажал кнопку
     * "Показать ещё".
     */
    private Answer moreAnswer(long userId) {
        FeedPosition position = storage.getFeedPosition(userId);
        if (position == null) {
            return new Answer("Сначала выберите ленту: /latest <url>");
        }
        return dialogStateHandler.showPage(userId, position.url(), position.shownCount());
    }

    /**
     * Добавляет ленту в список пользователя, а если ссылки нет, начинает
     * диалог добавления ленты.
     */
    private String addFeed(long userId, String url) {
        if (url.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_URL, ""));
            return ASK_URL_MESSAGE;
        }
        if (!dialogStateHandler.isFeed(url)) {
            return NOT_FOUND_MESSAGE;
        }
        if (!storage.add(userId, new Feed("", url))) {
            return DialogStateHandler.DUPLICATE_MESSAGE;
        }
        return "Лента добавлена";
    }

    /**
     * Возвращает ответ на обычный текст: если у пользователя есть активный
     * диалог, текст передаётся обработчику шагов диалога.
     */
    private Answer dialogAnswer(long userId, String text) {
        DialogState state = storage.getDialogState(userId);
        if (state.step() == DialogStep.NONE) {
            return new Answer(UNKNOWN_MESSAGE);
        }
        return dialogStateHandler.answer(userId, state, text);
    }

    /**
     * Отменяет текущий диалог и возвращает ответ пользователю.
     */
    private String cancelAnswer(long userId) {
        DialogStep step = storage.getDialogState(userId).step();
        if (step == DialogStep.NONE) {
            return "Нечего отменять";
        }
        storage.setDialogState(userId, DialogState.NO_DIALOG);
        return switch (step) {
            case WAITING_URL, WAITING_NAME -> "Добавление отменено";
            case WAITING_INTERVAL, WAITING_QUIET_FROM, WAITING_QUIET_TO -> "Настройка отменена";
            case WAITING_SEARCH_FEED, WAITING_SEARCH_QUERY -> "Поиск отменен";
            default -> "Действие отменено";
        };
    }

    /**
     * Удаляет ленту из списка пользователя по названию или ссылке.
     */
    private String removeFeed(long userId, String nameOrUrl) {
        if (nameOrUrl.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_REMOVE, ""));
            return ASK_REMOVE_MESSAGE;
        }
        if (!storage.remove(userId, dialogStateHandler.findFeedUrl(userId, nameOrUrl))) {
            return "Такой ленты нет в списке";
        }
        return "Лента удалена";
    }

    /**
     * Возвращает нумерованный список лент пользователя.
     */
    private String listFeeds(long userId) {
        List<Feed> feeds = storage.getAll(userId);
        if (feeds.isEmpty()) {
            return "Подписок пока нет. Добавь ленту командой /add <url>";
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < feeds.size(); i++) {
            lines.add((i + 1) + ". " + formatFeed(feeds.get(i)));
        }
        return String.join("\n", lines);
    }

    /**
     * Возвращает ленту для показа: название и ссылка, а если названия нет,
     * только ссылка.
     */
    private String formatFeed(Feed feed) {
        if (feed.name().isEmpty()) {
            return feed.url();
        }
        return feed.name() + " — " + feed.url();
    }

    /**
     * Показывает первую страницу новостей ленты по названию или ссылке.
     */
    private Answer latest(long userId, String nameOrUrl) {
        if (nameOrUrl.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_LATEST, ""));
            return new Answer(ASK_LATEST_MESSAGE);
        }
        return dialogStateHandler.showPage(userId,
                dialogStateHandler.findFeedUrl(userId, nameOrUrl), 0);
    }

    /**
     * Ищет новости по ключевым словам из параметров команды, а если
     * параметров нет, начинает диалог поиска. Первый параметр — область
     * поиска: all или название ленты (из нескольких слов — в кавычках),
     * остальные параметры — ключевые слова.
     */
    private Answer search(long userId, String argument) {
        dialogStateHandler.clearSearch(userId);
        if (argument.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_SEARCH_FEED, ""));
            return new Answer(ASK_SEARCH_FEED_MESSAGE);
        }
        String scope;
        String query;
        if (argument.startsWith("\"")) {
            int end = argument.indexOf('"', 1);
            if (end < 0) {
                return new Answer("Название в кавычках должно заканчиваться закрывающей кавычкой");
            }
            scope = argument.substring(1, end);
            query = argument.substring(end + 1).trim();
        } else {
            String[] tokens = argument.split("\\s+", 2);
            scope = tokens[0];
            query = tokens.length > 1 ? tokens[1].trim() : "";
        }
        return dialogStateHandler.searchAnswer(userId, scope, query);
    }

    /**
     * Меняет размер страницы и возвращает ответ пользователю.
     */
    private String pageSizeAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_PAGE_SIZE, ""));
            return ASK_PAGE_SIZE_MESSAGE;
        }
        if (!dialogStateHandler.changePageSize(userId, argument)) {
            return DialogStateHandler.PAGE_SIZE_ERROR_MESSAGE;
        }
        return "Размер страницы изменён: " + storage.getPageSize(userId);
    }

    /**
     * Задаёт интервал проверки лент из параметра команды, а если параметра
     * нет, начинает диалог настройки интервала.
     */
    private String intervalAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId, new DialogState(DialogStep.WAITING_INTERVAL, ""));
            return ASK_INTERVAL_MESSAGE;
        }
        return dialogStateHandler.intervalValueAnswer(userId, argument);
    }

    /**
     * Задаёт тихие часы из параметров команды: два часа через пробел или
     * "off" для отключения. Без параметров начинает диалог настройки тихих
     * часов.
     */
    private String quietAnswer(long userId, String argument) {
        if (argument.isEmpty()) {
            storage.setDialogState(userId,
                    new DialogState(DialogStep.WAITING_QUIET_FROM, ""));
            return ASK_QUIET_FROM_MESSAGE;
        }
        if (argument.equals("off")) {
            return dialogStateHandler.disableQuietHours(userId);
        }
        String[] hours = argument.split("\\s+");
        if (hours.length == 2) {
            Integer from = dialogStateHandler.parseHour(hours[0]);
            Integer to = dialogStateHandler.parseHour(hours[1]);
            if (from != null && to != null) {
                return dialogStateHandler.saveQuietHours(userId, from, to);
            }
        }
        return "Укажите тихие часы двумя числами от 0 до 23: /quiet <с> <до>";
    }

    /**
     * Показывает текущие настройки рассылки пользователя.
     */
    private String settingsAnswer(long userId) {
        MailingSettings settings = storage.getMailingSettings(userId);
        String quiet = "не настроены";
        if (settings.getQuietFromHour() != null) {
            quiet = "с " + dialogStateHandler.formatHour(settings.getQuietFromHour())
                    + " до " + dialogStateHandler.formatHour(settings.getQuietToHour());
        }
        return "Проверка лент: "
                + settings.getInterval().format()
                + ". Тихие часы: " + quiet;
    }
}
