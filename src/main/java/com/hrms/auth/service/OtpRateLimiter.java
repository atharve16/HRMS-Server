package com.hrms.auth.service;

import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.config.AuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OtpRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AuthProperties properties;
    private final Clock clock;
    private final AtomicLong cleanupCounter = new AtomicLong();

    public OtpRateLimiter(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(String clientAddress) {
        Instant now = clock.instant();
        Window window = windows.compute(clientAddress, (key, current) -> {
            if (current == null || !now.isBefore(current.startedAt.plus(properties.rateLimit().window()))) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt, current.count + 1);
        });
        if (cleanupCounter.incrementAndGet() % 100 == 0) {
            windows.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().startedAt.plus(properties.rateLimit().window())));
        }
        if (window.count > properties.rateLimit().maxRequestsPerIp()) {
            throw new ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests; please try again later");
        }
    }

    private record Window(Instant startedAt, int count) {
    }
}
