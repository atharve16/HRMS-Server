package com.hrms.office.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CreateOfficeLocationRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,20}") String locationCode,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotBlank String addressLine1, String addressLine2,
        @NotBlank String city, @NotBlank String state, @NotBlank String postalCode, @NotBlank String country,
        @NotNull @Valid CoordinatesRequest coordinates,
        @Positive @DecimalMax("10000") Double geofenceRadiusMeters,
        @Positive @DecimalMax("1000") Double maximumGpsAccuracyMeters) { }
