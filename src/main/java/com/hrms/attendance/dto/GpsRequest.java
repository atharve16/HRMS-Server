package com.hrms.attendance.dto;
import jakarta.validation.constraints.*;
public record GpsRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,@NotNull @Positive @DecimalMax("1000") Double accuracyMeters) { }
