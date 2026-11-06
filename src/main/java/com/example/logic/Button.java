package com.example.logic;

/**
 * Кнопка под сообщением бота: подпись на кнопке и данные, которые придут
 * боту при нажатии.
 */
public record Button(String text, String data) {
}
