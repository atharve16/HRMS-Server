package com.hrms.attendance;

import com.hrms.attendance.dto.GpsRequest;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.attendance.service.*;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.config.AttendanceProperties;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttendanceMidnightClosureTest {
    private AttendanceRepository attendanceRepository;
    private EmployeeRepository employeeRepository;
    private AttendanceScheduleService scheduleService;
    private GeofenceService geofenceService;
    private AttendanceCalculationService calculationService;
    private CompanyTimeProvider timeProvider;
    private AuditService auditService;
    private AttendanceProperties attendanceProperties;
    private AttendanceService attendanceService;
    private AttendanceMidnightScheduler scheduler;

    private final ZoneId companyZone = ZoneId.of("Asia/Kolkata");
    private final LocalDate today = LocalDate.of(2026, 11, 16);
    private final LocalDate yesterday = LocalDate.of(2026, 11, 15);
    private final Instant now = today.atTime(9, 0).atZone(companyZone).toInstant();

    @BeforeEach
    void setUp() {
        attendanceRepository = mock(AttendanceRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        scheduleService = mock(AttendanceScheduleService.class);
        geofenceService = mock(GeofenceService.class);
        calculationService = new AttendanceCalculationService();
        timeProvider = mock(CompanyTimeProvider.class);
        auditService = mock(AuditService.class);
        attendanceProperties = mock(AttendanceProperties.class);

        when(timeProvider.companyZoneId()).thenReturn(companyZone);
        when(timeProvider.companyDate()).thenReturn(today);
        when(timeProvider.now()).thenReturn(now);

        attendanceService = new AttendanceService(
                attendanceRepository, employeeRepository, scheduleService,
                geofenceService, calculationService, timeProvider,
                auditService, attendanceProperties
        );

        scheduler = new AttendanceMidnightScheduler(attendanceService, timeProvider);
    }

    private AttendanceRecord createRecord(String empId, LocalDate date, Instant checkIn) {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(UUID.randomUUID().toString());
        record.setEmployeeId(empId);
        record.setWorkDate(date);
        record.setAttendanceStatus(AttendanceStatus.INCOMPLETE);

        ShiftSnapshot shift = new ShiftSnapshot();
        shift.setScheduledStartAt(date.atTime(9, 0).atZone(companyZone).toInstant());
        shift.setScheduledEndAt(date.atTime(18, 0).atZone(companyZone).toInstant());
        shift.setGracePeriodMinutes(15);
        shift.setEarlyDepartureGraceMinutes(10);
        shift.setExpectedBreakMinutes(60);
        record.setShiftSnapshot(shift);

        AttendanceSession session = new AttendanceSession();
        session.setSessionId("sess-1");
        session.setCheckInAt(checkIn);
        session.setStatus(SessionStatus.OPEN);
        session.setAttendanceDate(date);
        record.getSessions().add(session);

        return record;
    }

    @Test
    void midnightClosureAutomaticallyClosesPastOpenSessions() {
        Instant yesterdayCheckIn = yesterday.atTime(9, 0).atZone(companyZone).toInstant();
        AttendanceRecord yesterdayRecord = createRecord("emp-1", yesterday, yesterdayCheckIn);

        when(attendanceRepository.findBySessionsStatus(SessionStatus.OPEN))
                .thenReturn(List.of(yesterdayRecord));

        int closed = attendanceService.closeUnfinishedSessionsBefore(today);

        assertThat(closed).isEqualTo(1);

        AttendanceSession closedSession = yesterdayRecord.getSessions().get(0);
        assertThat(closedSession.getStatus()).isEqualTo(SessionStatus.INCOMPLETE_CHECKOUT);
        assertThat(closedSession.getClosureType()).isEqualTo("AUTOMATIC_MIDNIGHT");
        assertThat(closedSession.getClosureReason()).contains("midnight");
        assertThat(closedSession.getCheckOutLocation()).isNull();

        // End of original attendance date in company timezone:
        Instant expectedEndOfDate = yesterday.atTime(LocalTime.MAX).atZone(companyZone).toInstant();
        assertThat(closedSession.getCheckOutAt()).isEqualTo(expectedEndOfDate);
        // Session does NOT extend into next date
        assertThat(closedSession.getCheckOutAt()).isBefore(today.atStartOfDay(companyZone).toInstant());

        // Working hours must be zero, status must be HALF_DAY
        assertThat(yesterdayRecord.getTotalWorkedMinutes()).isZero();
        assertThat(yesterdayRecord.getOvertimeMinutes()).isZero();
        assertThat(yesterdayRecord.getAttendanceStatus()).isEqualTo(AttendanceStatus.HALF_DAY);

        verify(attendanceRepository).save(yesterdayRecord);
    }

    @Test
    void repeatedExecutionIsIdempotent() {
        Instant yesterdayCheckIn = yesterday.atTime(9, 0).atZone(companyZone).toInstant();
        AttendanceRecord yesterdayRecord = createRecord("emp-1", yesterday, yesterdayCheckIn);

        when(attendanceRepository.findBySessionsStatus(SessionStatus.OPEN))
                .thenReturn(List.of(yesterdayRecord))
                .thenReturn(List.of()); // second run finds none

        int closedFirst = attendanceService.closeUnfinishedSessionsBefore(today);
        assertThat(closedFirst).isEqualTo(1);

        int closedSecond = attendanceService.closeUnfinishedSessionsBefore(today);
        assertThat(closedSecond).isEqualTo(0);

        // Saved once only
        verify(attendanceRepository, times(1)).save(yesterdayRecord);
    }

    @Test
    void missedExecutionsAreRecovered() {
        LocalDate twoDaysAgo = today.minusDays(2);
        AttendanceRecord oldRecord = createRecord("emp-2", twoDaysAgo, twoDaysAgo.atTime(9, 0).atZone(companyZone).toInstant());

        when(attendanceRepository.findBySessionsStatus(SessionStatus.OPEN))
                .thenReturn(List.of(oldRecord));

        scheduler.onApplicationReady();

        assertThat(oldRecord.getSessions().get(0).getStatus()).isEqualTo(SessionStatus.INCOMPLETE_CHECKOUT);
        assertThat(oldRecord.getAttendanceStatus()).isEqualTo(AttendanceStatus.HALF_DAY);
        assertThat(oldRecord.getTotalWorkedMinutes()).isZero();
        verify(attendanceRepository).save(oldRecord);
    }

    @Test
    void validCurrentDaySessionIsNotClosed() {
        Instant todayCheckIn = today.atTime(9, 0).atZone(companyZone).toInstant();
        AttendanceRecord todayRecord = createRecord("emp-3", today, todayCheckIn);

        when(attendanceRepository.findBySessionsStatus(SessionStatus.OPEN))
                .thenReturn(List.of(todayRecord));

        int closed = attendanceService.closeUnfinishedSessionsBefore(today);
        assertThat(closed).isZero();
        assertThat(todayRecord.getSessions().get(0).getStatus()).isEqualTo(SessionStatus.OPEN);
        verify(attendanceRepository, never()).save(todayRecord);
    }

    @Test
    void nextDayCheckInSucceedsDespiteUnresolvedPreviousIncompleteAttendance() {
        String empId = "emp-4";
        String userId = "user-4";
        Employee employee = new Employee();
        employee.setId(empId);
        employee.setUserId(userId);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setEmployeeCode("EMP004");
        employee.setFirstName("Alice");
        employee.setLastName("Smith");

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

        // Previous day was open
        AttendanceRecord yesterdayRecord = createRecord(empId, yesterday, yesterday.atTime(9, 0).atZone(companyZone).toInstant());
        when(attendanceRepository.findByEmployeeIdAndSessionsStatus(empId, SessionStatus.OPEN))
                .thenReturn(List.of(yesterdayRecord));

        // Shift snapshot and planned schedule for today
        ShiftSnapshot todayShift = new ShiftSnapshot();
        todayShift.setShiftId("shift-1");
        todayShift.setShiftName("Day Shift");
        todayShift.setStartTime(LocalTime.of(9, 0));
        todayShift.setEndTime(LocalTime.of(18, 0));
        todayShift.setScheduledStartAt(today.atTime(9, 0).atZone(companyZone).toInstant());
        todayShift.setScheduledEndAt(today.atTime(18, 0).atZone(companyZone).toInstant());
        todayShift.setGracePeriodMinutes(15);
        todayShift.setEarlyDepartureGraceMinutes(10);
        todayShift.setExpectedBreakMinutes(60);

        OfficeSnapshot office = new OfficeSnapshot();
        office.setOfficeLocationId("office-1");

        AttendanceScheduleService.Schedule planned = new AttendanceScheduleService.Schedule(
                today, null, todayShift, office,
                todayShift.getScheduledStartAt(), todayShift.getScheduledEndAt()
        );

        when(scheduleService.resolveCheckIn(empId)).thenReturn(planned);
        when(geofenceService.validate(any(), any())).thenReturn(10.0);
        when(attendanceRepository.findByEmployeeIdAndWorkDate(empId, today)).thenReturn(Optional.empty());

        AuthPrincipal principal = new AuthPrincipal(userId, "alice@example.com", com.hrms.auth.entity.UserRole.EMPLOYEE);
        GpsRequest gps = new GpsRequest(23.18, 79.98, 10.0);

        var response = attendanceService.checkIn(principal, gps);

        // Previous record was auto-closed as INCOMPLETE_CHECKOUT / HALF_DAY
        assertThat(yesterdayRecord.getSessions().get(0).getStatus()).isEqualTo(SessionStatus.INCOMPLETE_CHECKOUT);
        assertThat(yesterdayRecord.getAttendanceStatus()).isEqualTo(AttendanceStatus.HALF_DAY);
        assertThat(yesterdayRecord.getTotalWorkedMinutes()).isZero();

        // New day session was successfully created
        assertThat(response).isNotNull();
        assertThat(response.workDate()).isEqualTo(today);
        assertThat(response.sessions()).hasSize(1);
        assertThat(response.sessions().get(0).getStatus()).isEqualTo(SessionStatus.OPEN);
    }

    @Test
    void multipleSessionsOnSameDaySupportSubsequentCheckIn() {
        String empId = "emp-5";
        String userId = "user-5";
        Employee employee = new Employee();
        employee.setId(empId);
        employee.setUserId(userId);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setEmployeeCode("EMP005");
        employee.setFirstName("Bob");
        employee.setLastName("Jones");

        when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndSessionsStatus(empId, SessionStatus.OPEN))
                .thenReturn(List.of());

        ShiftSnapshot todayShift = new ShiftSnapshot();
        todayShift.setShiftId("shift-1");
        todayShift.setShiftName("Day Shift");
        todayShift.setStartTime(LocalTime.of(9, 0));
        todayShift.setEndTime(LocalTime.of(18, 0));
        todayShift.setScheduledStartAt(today.atTime(9, 0).atZone(companyZone).toInstant());
        todayShift.setScheduledEndAt(today.atTime(18, 0).atZone(companyZone).toInstant());
        todayShift.setGracePeriodMinutes(15);
        todayShift.setEarlyDepartureGraceMinutes(10);
        todayShift.setExpectedBreakMinutes(60);

        OfficeSnapshot office = new OfficeSnapshot();
        office.setOfficeLocationId("office-1");

        AttendanceScheduleService.Schedule planned = new AttendanceScheduleService.Schedule(
                today, null, todayShift, office,
                todayShift.getScheduledStartAt(), todayShift.getScheduledEndAt()
        );

        when(scheduleService.resolveCheckIn(empId)).thenReturn(planned);
        when(geofenceService.validate(any(), any())).thenReturn(10.0);

        // Existing completed session for today
        AttendanceRecord todayRecord = new AttendanceRecord();
        todayRecord.setId("rec-today-multiple");
        todayRecord.setEmployeeId(empId);
        todayRecord.setWorkDate(today);
        AttendanceSession s1 = new AttendanceSession();
        s1.setSessionId("s1");
        s1.setCheckInAt(today.atTime(9, 0).atZone(companyZone).toInstant());
        s1.setCheckOutAt(today.atTime(13, 0).atZone(companyZone).toInstant());
        s1.setStatus(SessionStatus.COMPLETED);
        todayRecord.getSessions().add(s1);

        when(attendanceRepository.findByEmployeeIdAndWorkDate(empId, today)).thenReturn(Optional.of(todayRecord));

        AuthPrincipal principal = new AuthPrincipal(userId, "bob@example.com", com.hrms.auth.entity.UserRole.EMPLOYEE);
        GpsRequest gps = new GpsRequest(23.18, 79.98, 10.0);

        var response = attendanceService.checkIn(principal, gps);

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions().get(0).getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.sessions().get(1).getStatus()).isEqualTo(SessionStatus.OPEN);
    }
}
