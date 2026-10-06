package com.example.cvm.util;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class TransIdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TransIdGenerator(Clock clock) {
        this.clock = clock;
    }

    /** 17-digit timestamp followed by 3 random digits. */
    public String next() {
        return LocalDateTime.now(clock).format(FMT) + String.format("%03d", random.nextInt(1000));
    }
}
