package com.hrms.office.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record UpdateOfficeLocationRequest(
        @Pattern(regexp = "[A-Za-z0-9_-]{2,20}") String locationCode,
        @Size(max = 120) String name, @Size(max = 1000) String description,
        String addressLine1, String addressLine2, String city, String state, String postalCode, String country,
        @Valid CoordinatesRequest coordinates,
        @Positive @DecimalMax("10000") Double geofenceRadiusMeters,
        @Positive @DecimalMax("1000") Double maximumGpsAccuracyMeters) { }
