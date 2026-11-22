package com.example.mailing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Управляемые часы: время двигается вперёд только по команде.
 */
class MutableClock extends Clock {

    /**
     * Текущий момент времени.
     */
    private Instant instant;

    /**
     * Создаёт часы, показывающие указанный момент.
     */
    MutableClock(Instant instant) {
        this.instant = instant;
    }

    /**
     * Сдвигает время вперёд.
     */
    void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
