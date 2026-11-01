package com.example.storage;

/**
 * Тесты для хранилища данных пользователей в памяти
 */
public class MemoryStorageTest extends StorageTest {

    @Override
    protected Storage createStorage() {
        return new MemoryStorage();
    }
}
