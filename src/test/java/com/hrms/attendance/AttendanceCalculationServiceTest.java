package com.hrms.attendance;

import com.hrms.attendance.dto.GpsRequest;
import com.hrms.attendance.model.*;
import com.hrms.attendance.service.AttendanceCalculationService;
import com.hrms.attendance.service.GeofenceService;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class AttendanceCalculationServiceTest {
    private final AttendanceCalculationService calculator = new AttendanceCalculationService();
    private AttendanceRecord record(Instant start, Instant end) { AttendanceRecord a=new AttendanceRecord(); ShiftSnapshot s=new ShiftSnapshot(); s.setScheduledStartAt(start);s.setScheduledEndAt(end);s.setGracePeriodMinutes(15);s.setEarlyDepartureGraceMinutes(10);s.setExpectedBreakMinutes(60);s.setFlexible(false);a.setShiftSnapshot(s);return a; }
    @Test void calculatesMultipleSessionsBreaksAndLate() { Instant shiftStart=Instant.parse("2026-11-01T03:30:00Z"), shiftEnd=Instant.parse("2026-11-01T11:30:00Z"); AttendanceRecord a=record(shiftStart,shiftEnd); AttendanceSession one=session("s1",Instant.parse("2026-11-01T03:55:00Z"),Instant.parse("2026-11-01T07:30:00Z")); AttendanceSession two=session("s2",Instant.parse("2026-11-01T08:30:00Z"),Instant.parse("2026-11-01T12:00:00Z"));a.getSessions().add(one);a.getSessions().add(two);calculator.recalculate(a);assertThat(a.getTotalWorkedMinutes()).isEqualTo(425);assertThat(a.getTotalBreakMinutes()).isEqualTo(60);assertThat(a.getLateByMinutes()).isEqualTo(10);assertThat(a.getOvertimeMinutes()).isEqualTo(30);assertThat(a.getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT); }
    @Test void openSessionIsIncompleteAndNotWorked() { AttendanceRecord a=record(Instant.parse("2026-11-01T03:30:00Z"),Instant.parse("2026-11-01T11:30:00Z")); AttendanceSession s=session("s",Instant.parse("2026-11-01T03:30:00Z"),null);a.getSessions().add(s);calculator.recalculate(a);assertThat(a.getTotalWorkedMinutes()).isZero();assertThat(a.getAttendanceStatus()).isEqualTo(AttendanceStatus.INCOMPLETE); }
    @Test void overlappingOrZeroDurationIsRejected() { AttendanceRecord a=record(Instant.parse("2026-11-01T03:30:00Z"),Instant.parse("2026-11-01T11:30:00Z"));a.getSessions().add(session("one",Instant.parse("2026-11-01T03:30:00Z"),Instant.parse("2026-11-01T05:00:00Z")));a.getSessions().add(session("two",Instant.parse("2026-11-01T04:30:00Z"),Instant.parse("2026-11-01T06:00:00Z")));assertThatThrownBy(()->calculator.recalculate(a)).isInstanceOf(IllegalArgumentException.class); }
    @Test void geofenceUsesHaversineAndAccuracy() { GeofenceService g=new GeofenceService(); OfficeSnapshot o=new OfficeSnapshot();o.setCoordinates(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(79.9864,23.1815));o.setGeofenceRadiusMeters(100);o.setMaximumGpsAccuracyMeters(50);assertThat(g.validate(new GpsRequest(23.1815,79.9864,10.0),o)).isLessThan(1);assertThatThrownBy(()->g.validate(new GpsRequest(23.1815,79.9864,60.0),o)).isInstanceOf(RuntimeException.class);assertThatThrownBy(()->g.validate(new GpsRequest(24.0,79.0,10.0),o)).isInstanceOf(RuntimeException.class); }
    private AttendanceSession session(String id,Instant in,Instant out){AttendanceSession s=new AttendanceSession();s.setSessionId(id);s.setCheckInAt(in);s.setCheckOutAt(out);s.setStatus(out==null?SessionStatus.OPEN:SessionStatus.COMPLETED);return s;}
}
