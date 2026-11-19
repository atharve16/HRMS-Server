package com.hrms.attendance.dto;
import jakarta.validation.constraints.*;
public record GpsRequest(
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        Double latitude,
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        Double longitude,
        @NotNull(message = "GPS accuracy is required")
        @Positive(message = "GPS accuracy must be greater than 0")
        @DecimalMax(value = "1000", message = "GPS accuracy must not exceed 1000 meters")
        Double accuracyMeters) { }
