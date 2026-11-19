package com.hrms.attendance;

import com.hrms.attendance.dto.GpsRequest;
import com.hrms.attendance.model.OfficeSnapshot;
import com.hrms.attendance.service.GeofenceService;
import com.hrms.common.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeofenceServiceTest {
    @Test
    void rejectsNonFiniteAccuracyWithoutWeakeningGpsPolicy() {
        OfficeSnapshot office = new OfficeSnapshot();
        office.setCoordinates(new GeoJsonPoint(79.98, 23.18));
        office.setMaximumGpsAccuracyMeters(50);
        office.setGeofenceRadiusMeters(100);

        assertThatThrownBy(() -> new GeofenceService().validate(
                new GpsRequest(23.18, 79.98, Double.NaN), office))
                .isInstanceOf(ApiException.class)
                .hasMessage("GPS accuracy is outside the permitted limit");
    }
}
