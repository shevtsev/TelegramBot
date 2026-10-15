package com.example;

/**
 * Позиция чтения ленты пользователем: какая лента и сколько новостей уже
 * показано.
 */
public record Reading(String url, int offset) {
}
