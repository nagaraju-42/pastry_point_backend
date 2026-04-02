package com.bakeryq.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Generates a unique, human-readable order number.
     * Format: BQ-20240101-0042
     */
    public String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = counter.incrementAndGet() % 9999;
        return String.format("BQ-%s-%04d", date, seq);
    }
}
