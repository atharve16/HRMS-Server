package com.hrms.common.utils;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyTimeProviderTest {

    @Test
    void derivesCompanyDateWithoutChangingUtcStorageTime() {
        Instant instant = Instant.parse("2026-10-31T20:00:00Z");
        CompanyTimeProvider provider = new CompanyTimeProvider(
                Clock.fixed(instant, ZoneOffset.UTC),
                ZoneId.of("Asia/Kolkata")
        );

        assertThat(provider.now()).isEqualTo(instant);
        assertThat(provider.companyDate()).hasToString("2026-11-01");
        assertThat(provider.companyNow().getOffset()).isEqualTo(ZoneOffset.ofHoursMinutes(5, 30));
    }
}
