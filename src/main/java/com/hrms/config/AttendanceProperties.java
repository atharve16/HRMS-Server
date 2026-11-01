package com.hrms.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.attendance")
public record AttendanceProperties(
        @Min(0) @Max(720) int earlyCheckInMinutes,
        @Min(0) @Max(1440) int lateCheckInWindowMinutes,
        @Min(0) @Max(720) int overnightCheckoutGraceMinutes,
        @Min(1) @Max(72) int maxSessionDurationHours) { }
