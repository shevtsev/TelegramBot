package com.example.storage;

/**
 * Настройки рассылки новостей: интервал проверки лент в минутах и тихие часы
 * (часы начала и конца паузы; null, если тихие часы не настроены).
 */
public record MailingSettings(int checkIntervalMinutes, Integer quietFromHour,
        Integer quietToHour) {

    /**
     * Настройки по умолчанию: проверка каждые 30 минут, тихие часы не
     * настроены.
     */
    public static final MailingSettings DEFAULT = new MailingSettings(30, null, null);
}
