package com.hrms.common.responses;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ClockSource {

    private final Clock clock;

    public ClockSource(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
