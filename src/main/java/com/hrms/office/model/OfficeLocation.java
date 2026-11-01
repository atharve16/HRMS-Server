package com.hrms.office.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;

@Document("office_locations")
public class OfficeLocation {
    @Id private String id;
    @Indexed(unique = true) private String locationCode;
    private String name;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint coordinates;
    private double geofenceRadiusMeters;
    private double maximumGpsAccuracyMeters;
    @Indexed private OfficeStatus status;
    private String createdBy;
    private String updatedBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String value) { locationCode = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String value) { addressLine1 = value; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String value) { addressLine2 = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = value; }
    public String getCountry() { return country; }
    public void setCountry(String value) { country = value; }
    public GeoJsonPoint getCoordinates() { return coordinates; }
    public void setCoordinates(GeoJsonPoint value) { coordinates = value; }
    public double getGeofenceRadiusMeters() { return geofenceRadiusMeters; }
    public void setGeofenceRadiusMeters(double value) { geofenceRadiusMeters = value; }
    public double getMaximumGpsAccuracyMeters() { return maximumGpsAccuracyMeters; }
    public void setMaximumGpsAccuracyMeters(double value) { maximumGpsAccuracyMeters = value; }
    public OfficeStatus getStatus() { return status; }
    public void setStatus(OfficeStatus value) { status = value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy = value; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String value) { updatedBy = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
