package com.example;

/**
 * Одна новость из RSS-ленты.
 */
public class FeedItem {

    private final String title;
    private final String link;
    private final String pubDate;

    /**
     * Создаёт новость с заголовком, ссылкой и датой публикации.
     */
    public FeedItem(String title, String link, String pubDate) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
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
}
