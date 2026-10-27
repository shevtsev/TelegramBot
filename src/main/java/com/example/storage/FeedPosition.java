package com.example.storage;

/**
 * Позиция пользователя в ленте: ссылка на ленту и сколько новостей из неё уже
 * показано.
 */
public record FeedPosition(String url, int shownCount) {
}
