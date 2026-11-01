package com.hrms.common.responses;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void createsSuccessEnvelopeUsingInjectedUtcClock() {
        Instant instant = Instant.parse("2026-10-31T10:00:00Z");
        ClockSource clockSource = new ClockSource(Clock.fixed(instant, ZoneOffset.UTC));

        ApiResponse<Map<String, String>> response =
                ApiResponse.success("Operation successful", Map.of("id", "123"), clockSource);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Operation successful");
        assertThat(response.data()).containsEntry("id", "123");
        assertThat(response.timestamp()).isEqualTo(instant);
    }
}
