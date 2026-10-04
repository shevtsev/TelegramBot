package com.example;

import org.junit.jupiter.api.Test;

public class BotLogicTest {
    @Test
    void testUserAnswer() {
        String startResponse = BotLogic.UserAnswer("Привет");
        assert startResponse.equals("Пользователь написал: Привет");
    }
}