package com.hrms.auth;

import com.hrms.config.AuthProperties;

import java.time.Duration;

public final class AuthTestProperties {

    private AuthTestProperties() {
    }

    public static AuthProperties create() {
        return new AuthProperties(
                new AuthProperties.Jwt("jwt-test-secret-at-least-thirty-two-characters", "hrms-test",
                        Duration.ofMinutes(15), Duration.ofDays(30)),
                new AuthProperties.Otp("otp-test-secret-at-least-thirty-two-characters",
                        Duration.ofMinutes(5), Duration.ofSeconds(60), 5),
                new AuthProperties.Cookie("hrms_refresh_token", "/api/v1/auth", false, "Lax", ""),
                new AuthProperties.Mail("noreply@example.com", "HRMS Test"),
                new AuthProperties.RateLimit(10, Duration.ofMinutes(5)),
                new AuthProperties.Bootstrap("")
        );
    }
}
