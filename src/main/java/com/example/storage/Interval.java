package com.example.storage;

import java.util.Objects;

/**
 * Интервал проверки лент: от 1 секунды до одной недели.
 */
public class Interval {

    /**
     * Максимальная длительность интервала в секундах (одна неделя).
     */
    private static final int MAX_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Длительность интервала в секундах.
     */
    private final int seconds;

    /**
     * Создаёт интервал указанной длительности в секундах.
     */
    public Interval(int seconds) {
        if (seconds < 1 || seconds > MAX_SECONDS) {
            throw new IllegalArgumentException(
                    "Интервал должен быть от 1 секунды до недели: " + seconds);
        }
        this.seconds = seconds;
    }

    /**
     * Возвращает длительность интервала в секундах.
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Возвращает описание интервала для ответов бота: "каждые 30 минут",
     * "каждый час", "каждую секунду".
     */
    public String format() {
        if (seconds % (24 * 60 * 60) == 0) {
            return every(seconds / (24 * 60 * 60), "каждый день", "день", "дня", "дней");
        }
        if (seconds % (60 * 60) == 0) {
            return every(seconds / (60 * 60), "каждый час", "час", "часа", "часов");
        }
        if (seconds % 60 == 0) {
            return every(seconds / 60, "каждую минуту", "минуту", "минуты", "минут");
        }
        return every(seconds, "каждую секунду", "секунду", "секунды", "секунд");
    }

    /**
     * Собирает описание интервала из количества и форм единицы времени.
     */
    private String every(int count, String singular, String one,
            String few, String many) {
        if (count == 1) {
            return singular;
        }
        int lastTwo = count % 100;
        int last = count % 10;
        String form;
        if (last == 1 && lastTwo != 11) {
            form = one;
        } else if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) {
            form = few;
        } else {
            form = many;
        }
        return "каждые " + count + " " + form;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Interval interval)) {
            return false;
        }
        return seconds == interval.seconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds);
    }
}
