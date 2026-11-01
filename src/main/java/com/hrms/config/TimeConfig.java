package com.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId companyZoneId(ApplicationProperties properties) {
        return ZoneId.of(properties.company().timeZone());
    }
}
