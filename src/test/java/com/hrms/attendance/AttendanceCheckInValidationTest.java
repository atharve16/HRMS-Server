package com.hrms.attendance;

import com.hrms.attendance.dto.GpsRequest;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.attendance.service.*;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ConflictException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.config.AttendanceProperties;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttendanceCheckInValidationTest {
    private final AttendanceRepository records = mock(AttendanceRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AttendanceScheduleService schedule = mock(AttendanceScheduleService.class);
    private final GeofenceService geofence = mock(GeofenceService.class);
    private final CompanyTimeProvider time = mock(CompanyTimeProvider.class);
    private final AuditService audit = mock(AuditService.class);
    private AttendanceService service;

    private final LocalDate workDate = LocalDate.of(2026, 11, 18);
    private final Instant now = Instant.parse("2026-11-18T05:00:00Z");
    private final AuthPrincipal principal = new AuthPrincipal("user-1", "employee@example.com", UserRole.EMPLOYEE);
    private final GpsRequest gps = new GpsRequest(23.18, 79.98, 10.0);

    @BeforeEach
    void setUp() {
        service = new AttendanceService(records, employees, schedule, geofence,
                new AttendanceCalculationService(), time, audit,
                new AttendanceProperties(120, 240, 240, 24));

        Employee employee = new Employee();
        employee.setId("employee-1");
        employee.setUserId(principal.userId());
        employee.setEmployeeCode("EMP001");
        employee.setFirstName("Test");
        employee.setLastName("Employee");
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        when(employees.findByUserId(principal.userId())).thenReturn(Optional.of(employee));
        when(time.companyDate()).thenReturn(workDate);
        when(time.now()).thenReturn(now);
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));
        when(records.findByEmployeeIdAndSessionsStatus("employee-1", SessionStatus.OPEN)).thenReturn(List.of());
        when(schedule.resolveCheckIn("employee-1")).thenReturn(plannedSchedule());
        when(geofence.validate(any(), any())).thenReturn(10.0);
    }

    @Test
    void normalEmployeeCanCheckInWithServerOwnedIdentityAndWorkDate() {
        when(records.findByEmployeeIdAndWorkDate("employee-1", workDate)).thenReturn(Optional.empty());

        var response = service.checkIn(principal, gps);

        assertThat(response.employeeId()).isEqualTo("employee-1");
        assertThat(response.workDate()).isEqualTo(workDate);
        assertThat(response.sessions()).singleElement().extracting(AttendanceSession::getStatus)
                .isEqualTo(SessionStatus.OPEN);
        verify(records).save(any(AttendanceRecord.class));
        verify(geofence).validate(gps, response.officeSnapshot());
    }

    @Test
    void existingOpenSessionCannotBeDuplicated() {
        AttendanceRecord existing = recordWithSession(SessionStatus.OPEN);
        when(records.findByEmployeeIdAndWorkDate("employee-1", workDate)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.checkIn(principal, gps))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An attendance session is already active");
        verify(records, never()).save(any(AttendanceRecord.class));
    }

    @Test
    void legacyRecordWithoutSessionsCanReceiveFirstSession() {
        AttendanceRecord legacy = new AttendanceRecord();
        legacy.setId("legacy-record");
        legacy.setEmployeeId("employee-1");
        legacy.setWorkDate(workDate);
        legacy.setSessions(null);
        when(records.findByEmployeeIdAndWorkDate("employee-1", workDate)).thenReturn(Optional.of(legacy));

        var response = service.checkIn(principal, gps);

        assertThat(response.sessions()).hasSize(1);
        assertThat(legacy.getSessions()).hasSize(1);
        verify(records).save(legacy);
    }

    private AttendanceScheduleService.Schedule plannedSchedule() {
        var shift = new ShiftSnapshot();
        shift.setScheduledStartAt(workDate.atTime(9, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        shift.setScheduledEndAt(workDate.atTime(18, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        shift.setGracePeriodMinutes(15);
        shift.setEarlyDepartureGraceMinutes(10);
        shift.setExpectedBreakMinutes(60);
        var office = new OfficeSnapshot();
        office.setOfficeLocationId("office-1");
        return new AttendanceScheduleService.Schedule(workDate, null, shift, office,
                shift.getScheduledStartAt(), shift.getScheduledEndAt());
    }

    private AttendanceRecord recordWithSession(SessionStatus status) {
        AttendanceRecord record = new AttendanceRecord();
        record.setId("record-1");
        record.setEmployeeId("employee-1");
        record.setWorkDate(workDate);
        AttendanceSession session = new AttendanceSession();
        session.setSessionId("session-1");
        session.setCheckInAt(now.minusSeconds(3600));
        session.setStatus(status);
        record.getSessions().add(session);
        return record;
    }
}
