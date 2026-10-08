package com.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

    private final BotLogic botLogic = new BotLogic();

    /**
     * Тестирование эхо ответа бота.
     */
    @Test
    void testEchoResponse() {
        String echoResponse = botLogic.userAnswer("Привет");
        Assertions.assertEquals("Пользователь написал: Привет", echoResponse);
    }
}