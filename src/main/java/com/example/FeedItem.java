package com.example;

/**
 * Одна новость из RSS-ленты.
 */
public class FeedItem {

    private final String title;
    private final String link;
    private final String pubDate;

    public FeedItem(String title, String link, String pubDate) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }
}
