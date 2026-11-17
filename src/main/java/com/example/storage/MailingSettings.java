package com.example.storage;

import java.util.Objects;

/**
 * Настройки рассылки новостей: интервал проверки лент и тихие часы
 * (часы начала и конца паузы; null, если тихие часы не настроены).
 * Параметры изменяются сеттерами.
 */
public class MailingSettings {

    /**
     * Интервал проверки лент.
     */
    private Interval interval;

    /**
     * Час начала тихих часов или null, если не настроены.
     */
    private Integer quietFromHour;

    /**
     * Час конца тихих часов или null, если не настроены.
     */
    private Integer quietToHour;

    /**
     * Создаёт настройки по умолчанию: проверка каждые 30 минут, тихие часы
     * не настроены.
     */
    public MailingSettings() {
        this(new Interval(1800), null, null);
    }

    /**
     * Создаёт настройки с указанными параметрами.
     */
    public MailingSettings(Interval interval, Integer quietFromHour,
            Integer quietToHour) {
        this.interval = interval;
        this.quietFromHour = quietFromHour;
        this.quietToHour = quietToHour;
    }

    /**
     * Возвращает интервал проверки лент.
     */
    public Interval getInterval() {
        return interval;
    }

    /**
     * Задаёт интервал проверки лент.
     */
    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    /**
     * Возвращает час начала тихих часов или null, если не настроены.
     */
    public Integer getQuietFromHour() {
        return quietFromHour;
    }

    /**
     * Задаёт час начала тихих часов или null для отключения тихих часов.
     */
    public void setQuietFromHour(Integer quietFromHour) {
        this.quietFromHour = quietFromHour;
    }

    /**
     * Возвращает час конца тихих часов или null, если не настроены.
     */
    public Integer getQuietToHour() {
        return quietToHour;
    }

    /**
     * Задаёт час конца тихих часов или null для отключения тихих часов.
     */
    public void setQuietToHour(Integer quietToHour) {
        this.quietToHour = quietToHour;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MailingSettings settings)) {
            return false;
        }
        return Objects.equals(interval, settings.interval)
                && Objects.equals(quietFromHour, settings.quietFromHour)
                && Objects.equals(quietToHour, settings.quietToHour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, quietFromHour, quietToHour);
    }
}
