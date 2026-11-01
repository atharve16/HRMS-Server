package com.hrms.attendance.model;

public class GpsEvidence {
    private double latitude;
    private double longitude;
    private double accuracyMeters;
    public GpsEvidence() { }
    public GpsEvidence(double latitude, double longitude, double accuracyMeters) { this.latitude=latitude;this.longitude=longitude;this.accuracyMeters=accuracyMeters; }
    public double getLatitude(){return latitude;} public void setLatitude(double v){latitude=v;}
    public double getLongitude(){return longitude;} public void setLongitude(double v){longitude=v;}
    public double getAccuracyMeters(){return accuracyMeters;} public void setAccuracyMeters(double v){accuracyMeters=v;}
}
