package com.hrms.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Otp otp,
        @Valid @NotNull Cookie cookie,
        @Valid @NotNull Mail mail,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Bootstrap bootstrap
) {
    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotBlank String issuer,
            @NotNull @PositiveDuration Duration accessExpiry,
            @NotNull @PositiveDuration Duration refreshExpiry
    ) {
    }

    public record Otp(
            @NotBlank @Size(min = 32) String hashSecret,
            @NotNull @PositiveDuration Duration expiry,
            @NotNull @PositiveDuration Duration resendCooldown,
            @Min(1) @Max(10) int maxAttempts
    ) {
    }

    public record Cookie(
            @NotBlank String refreshTokenName,
            @NotBlank String path,
            boolean secure,
            @NotBlank String sameSite,
            String domain
    ) {
    }

    public record Mail(
            @NotBlank @Email String from,
            @NotBlank String fromName
    ) {
    }

    public record RateLimit(
            @Min(1) int maxRequestsPerIp,
            @NotNull @PositiveDuration Duration window
    ) {
    }

    public record Bootstrap(
            @Email String initialAdminEmail
    ) {
    }
}
