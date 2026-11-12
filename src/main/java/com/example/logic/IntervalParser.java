package com.example.logic;

/**
 * Разбор и показ интервала проверки лент для рассылки новостей.
 */
class IntervalParser {

    /**
     * Максимальный интервал проверки лент в секундах (одна неделя).
     */
    private static final int MAX_SECONDS = 7 * 24 * 60 * 60;

    private IntervalParser() {
    }

    /**
     * Возвращает интервал в секундах по строке вида "30", "30s", "15m", "2h"
     * или "1d". Число без буквы считается минутами. Возвращает null, если
     * строка не похожа на интервал или интервал не от секунды до недели.
     */
    static Integer parseSeconds(String text) {
        if (!text.matches("\\d+[smhd]?")) {
            return null;
        }
        int unitIndex = text.length();
        long multiplier = 60;
        if (Character.isLetter(text.charAt(unitIndex - 1))) {
            unitIndex--;
            multiplier = switch (text.charAt(unitIndex)) {
                case 's' -> 1;
                case 'm' -> 60;
                case 'h' -> 60 * 60;
                default -> 24 * 60 * 60;
            };
        }
        long seconds = Long.parseLong(text.substring(0, unitIndex)) * multiplier;
        if (seconds < 1 || seconds > MAX_SECONDS) {
            return null;
        }
        return (int) seconds;
    }

    /**
     * Возвращает описание интервала для ответов бота: "каждые 30 минут",
     * "каждый час", "каждую секунду".
     */
    static String describe(int seconds) {
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
    private static String every(int count, String singular, String one,
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
}
