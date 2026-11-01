package com.hrms.common.controller;

import com.hrms.common.responses.ClockSource;
import com.hrms.common.utils.CompanyTimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {

    @Test
    void reportsAvailabilityAndConfiguredCompanyTimeZone() {
        Instant now = Instant.parse("2026-10-31T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        SystemController controller = new SystemController(
                new ClockSource(clock),
                new CompanyTimeProvider(clock, ZoneId.of("Asia/Kolkata"))
        );

        var response = controller.health();

        assertThat(response.success()).isTrue();
        assertThat(response.data())
                .containsEntry("status", "UP")
                .containsEntry("companyTimeZone", "Asia/Kolkata");
        assertThat(response.timestamp()).isEqualTo(now);
    }
}
