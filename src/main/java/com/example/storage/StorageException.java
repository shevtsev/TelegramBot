package com.example.storage;

/**
 * Ошибка при работе с хранилищем данных, например, когда база данных
 * недоступна.
 */
public class StorageException extends RuntimeException {

    /**
     * Создаёт ошибку хранилища с понятным сообщением и причиной.
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
