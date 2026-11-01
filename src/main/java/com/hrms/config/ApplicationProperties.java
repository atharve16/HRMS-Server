package com.hrms.config;

import com.hrms.common.validation.ValidTimeZone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @Valid @NotNull Company company,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Geofence geofence
) {
    public record Company(
            @NotBlank @ValidTimeZone String timeZone
    ) {
    }

    public record Cors(
            @NotEmpty List<@NotBlank String> allowedOrigins
    ) {
    }

    public record Geofence(
            @Positive double defaultRadiusMeters,
            @DecimalMin("1.0") @DecimalMax("1000.0") double maxGpsAccuracyMeters
    ) {
    }
}
