package com.hrms.common.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Component
public class CompanyTimeProvider {

    private final Clock utcClock;
    private final ZoneId companyZoneId;

    public CompanyTimeProvider(Clock utcClock, ZoneId companyZoneId) {
        this.utcClock = utcClock;
        this.companyZoneId = companyZoneId;
    }

    public Instant now() {
        return Instant.now(utcClock);
    }

    public ZonedDateTime companyNow() {
        return now().atZone(companyZoneId);
    }

    public LocalDate companyDate() {
        return companyNow().toLocalDate();
    }

    public ZoneId companyZoneId() {
        return companyZoneId;
    }
}
