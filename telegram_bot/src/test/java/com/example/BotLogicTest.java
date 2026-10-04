package com.example;

import org.junit.jupiter.api.Test;

public class BotLogicTest {
    @Test
    void testUserAnswer() {
        String helloResponse = BotLogic.UserAnswer("Привет");
        assert helloResponse.equals("Пользователь написал: Привет");
        String helpResponse = BotLogic.UserAnswer("/help");
        assert helpResponse.equals("""
                    Доступные команды:
                    /start - начать
                    /help - список команд
                    Любой другой текст повторяется""");
        String startResponse = BotLogic.UserAnswer("/start");
        assert startResponse.equals("Этот бот повторяет всё, что ты напишешь. Список команд: /help");
    }
}