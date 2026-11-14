package com.example.storage;

/**
 * Лента пользователя: название и ссылка (название пустое, если пользователь
 * его не задавал).
 */
public record Feed(String name, String url) {
}
