package com.hrms.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeZoneValidatorTest {

    private final TimeZoneValidator validator = new TimeZoneValidator();

    @Test
    void acceptsIanaTimeZone() {
        assertThat(validator.isValid("Asia/Kolkata", null)).isTrue();
    }

    @Test
    void rejectsUnknownTimeZone() {
        assertThat(validator.isValid("Mars/Olympus", null)).isFalse();
    }
}
