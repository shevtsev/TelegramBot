package com.example.feed;

import com.example.storage.Feed;

/**
 * Новость, найденная в одной из лент пользователя.
 *
 * @param feed лента, в которой нашлась новость
 * @param item найденная новость
 */
public record SearchResult(Feed feed, FeedItem item) {
}
