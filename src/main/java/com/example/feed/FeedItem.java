package com.example.feed;

/**
 * Одна новость из RSS-ленты.
 */
public class FeedItem {

    /**
     * Заголовок новости.
     */
    private final String title;

    /**
     * Ссылка на новость.
     */
    private final String link;

    /**
     * Дата публикации новости.
     */
    private final String pubDate;

    /**
     * Описание новости из ленты.
     */
    private final String description;

    /**
     * Создаёт новость с заголовком, ссылкой и датой публикации.
     */
    public FeedItem(String title, String link, String pubDate) {
        this(title, link, pubDate, "");
    }

    /**
     * Создаёт новость с заголовком, ссылкой, датой публикации и описанием.
     */
    public FeedItem(String title, String link, String pubDate, String description) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
    }

    /**
     * Возвращает заголовок новости.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Возвращает ссылку на новость.
     */
    public String getLink() {
        return link;
    }

    /**
     * Возвращает дату публикации новости в том виде, в каком она указана в ленте.
     */
    public String getPubDate() {
        return pubDate;
    }

    /**
     * Возвращает описание новости или пустую строку, если его нет.
     */
    public String getDescription() {
        return description;
    }
}
