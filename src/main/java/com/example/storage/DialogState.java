package com.example.storage;

/**
 * Состояние диалога добавления ленты: шаг и ссылка на уже найденную ленту
 * (ссылка пустая, пока пользователь её не прислал).
 */
public record DialogState(DialogStep step, String url) {

    /**
     * Состояние, когда пользователь не добавляет ленту.
     */
    public static final DialogState NO_DIALOG = new DialogState(DialogStep.NONE, "");
}
