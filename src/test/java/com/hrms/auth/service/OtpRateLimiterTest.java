package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpRateLimiterTest {

    @Test
    void limitsOtpRequestsPerClientAddressWithinWindow() {
        AuthProperties base = AuthTestProperties.create();
        AuthProperties properties = new AuthProperties(base.jwt(), base.otp(), base.cookie(), base.mail(),
                new AuthProperties.RateLimit(2, java.time.Duration.ofMinutes(5)), base.bootstrap());
        OtpRateLimiter limiter = new OtpRateLimiter(properties,
                Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC));

        limiter.check("127.0.0.1");
        limiter.check("127.0.0.1");

        assertThatThrownBy(() -> limiter.check("127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
        limiter.check("127.0.0.2");
    }
}
