package com.example.storage;

/**
 * Тесты для хранилища данных пользователей в памяти
 */
public class MemoryStorageTest extends StorageTest {

    @Override
    protected Storage createStorage() {
        return new MemoryStorage();
    }

    /**
     * Тестирование того, что у нового пользователя нет диалога добавления
     * ленты.
     */
    @Test
    void testNoDialogByDefault() {
        Assertions.assertEquals(DialogState.NO_DIALOG, storage.getDialogState(USER_ID));
    }

    /**
     * Тестирование сохранения шага диалога: у каждого пользователя свой шаг.
     */
    @Test
    void testDialogStateIsPerUser() {
        DialogState waitingName = new DialogState(DialogStep.WAITING_NAME, FEED_URL);
        storage.setDialogState(USER_ID, waitingName);
        Assertions.assertEquals(waitingName, storage.getDialogState(USER_ID));
        Assertions.assertEquals(DialogState.NO_DIALOG, storage.getDialogState(OTHER_USER_ID));
    }
}
