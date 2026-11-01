package com.hrms.office.dto;

import com.hrms.office.model.OfficeLocation;
import com.hrms.office.model.OfficeStatus;
import java.time.Instant;

public record OfficeLocationResponse(String id, String locationCode, String name, String description,
                                     String addressLine1, String addressLine2, String city, String state,
                                     String postalCode, String country, double latitude, double longitude,
                                     double geofenceRadiusMeters, double maximumGpsAccuracyMeters,
                                     OfficeStatus status, Instant createdAt, Instant updatedAt) {
    public static OfficeLocationResponse from(OfficeLocation o) {
        return new OfficeLocationResponse(o.getId(), o.getLocationCode(), o.getName(), o.getDescription(),
                o.getAddressLine1(), o.getAddressLine2(), o.getCity(), o.getState(), o.getPostalCode(), o.getCountry(),
                o.getCoordinates().getY(), o.getCoordinates().getX(), o.getGeofenceRadiusMeters(),
                o.getMaximumGpsAccuracyMeters(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
