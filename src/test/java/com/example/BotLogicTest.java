package com.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тесты для логики работы бота
 */
public class BotLogicTest {

    BotLogic botLogic = new BotLogic();

    /**
     * Тестирование эхо ответа бота.
     */
    @Test
    void testEchoResponse() {
        String echoResponse = botLogic.UserAnswer("Привет");
        Assertions.assertEquals("Пользователь написал: Привет", echoResponse);
    }

    /**
     * Тестирование команды /start.
     */
    @Test
    void testStartCommand() {
        String startResponse = botLogic.UserAnswer("/start");
        Assertions.assertEquals("Этот бот повторяет всё, что ты напишешь. Список команд: /help", startResponse);
    }

    /**
     * Тестирование команды /help.
     */
    @Test
    void testHelpCommand() {
        String helpResponse = botLogic.UserAnswer("/help");
        Assertions.assertEquals("""
                Доступные команды:
                /start - начать
                /help - список команд
                Любой другой текст повторяется""", helpResponse);
    }
}