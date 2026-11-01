package com.hrms.email;

import com.hrms.config.PositiveDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "brevo.api")
public record BrevoProperties(
        @NotBlank String url,
        @NotBlank String key,
        @NotNull @PositiveDuration Duration connectTimeout,
        @NotNull @PositiveDuration Duration responseTimeout
) {
}
