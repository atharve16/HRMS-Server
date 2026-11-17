package com.hrms.attendance;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.model.AssignmentStatus;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.attendance.dto.*;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.*;
import com.hrms.attendance.service.*;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ConflictException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.config.AttendanceProperties;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttendanceCorrectionServiceTest {
    private final AttendanceCorrectionRequestRepository requests = mock(AttendanceCorrectionRequestRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AssignmentService assignments = mock(AssignmentService.class);
    private final AttendanceScheduleService schedules = mock(AttendanceScheduleService.class);
    private final AttendanceCalculationService calculator = mock(AttendanceCalculationService.class);
    private final CompanyTimeProvider time = mock(CompanyTimeProvider.class);
    private final AuditService audit = mock(AuditService.class);
    private final AttendanceCorrectionService service = new AttendanceCorrectionService(requests, attendance, employees, assignments, schedules, calculator, time, new AttendanceProperties(120, 240, 240, 24), audit);
    private final AuthPrincipal employeePrincipal = new AuthPrincipal("user-1", "employee@example.com", UserRole.EMPLOYEE);
    private final AuthPrincipal adminPrincipal = new AuthPrincipal("admin-1", "admin@example.com", UserRole.ADMIN);

    @Test
    void employeeCanSubmitBothMissingPunches() {
        Employee employee = employee();
        when(employees.findByUserId("user-1")).thenReturn(Optional.of(employee));
        when(requests.existsByEmployeeIdAndAttendanceDateAndStatus(anyString(), any(), eq(AttendanceCorrectionStatus.PENDING))).thenReturn(false);
        when(attendance.findByEmployeeIdAndWorkDate(anyString(), any())).thenReturn(Optional.empty());
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));
        when(time.now()).thenReturn(Instant.parse("2026-11-14T10:00:00Z"));
        when(schedules.load(any(), any())).thenReturn(schedule());
        when(assignments.resolveHistorical(anyString(), any())).thenReturn(assignment());
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCorrectionResponse response = service.submit(new SubmitAttendanceCorrectionRequest(LocalDate.of(2026, 11, 14), MissingPunchType.BOTH, LocalTime.of(9, 15), LocalTime.of(18, 30), "Forgot both punches"), employeePrincipal);

        assertThat(response.status()).isEqualTo(AttendanceCorrectionStatus.PENDING);
        assertThat(response.requestedCheckInAt()).isEqualTo(Instant.parse("2026-11-14T03:45:00Z"));
        verify(requests).save(any());
    }

    @Test
    void employeeCanRequestMissingCheckoutForIncompleteSession() {
        Employee employee = employee();
        when(employees.findByUserId("user-1")).thenReturn(Optional.of(employee));
        when(requests.existsByEmployeeIdAndAttendanceDateAndStatus(anyString(), any(), eq(AttendanceCorrectionStatus.PENDING))).thenReturn(false);

        AttendanceRecord record = new AttendanceRecord();
        record.setId("rec-1");
        record.setWorkDate(LocalDate.of(2026, 11, 14));
        AttendanceSession session = new AttendanceSession();
        session.setSessionId("sess-1");
        session.setCheckInAt(Instant.parse("2026-11-14T03:30:00Z"));
        session.setStatus(SessionStatus.INCOMPLETE_CHECKOUT);
        record.getSessions().add(session);

        when(attendance.findByEmployeeIdAndWorkDate(anyString(), any())).thenReturn(Optional.of(record));
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));
        when(time.now()).thenReturn(Instant.parse("2026-11-15T10:00:00Z"));
        when(schedules.load(any(), any())).thenReturn(schedule());
        when(assignments.resolveHistorical(anyString(), any())).thenReturn(assignment());
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCorrectionResponse response = service.submit(new SubmitAttendanceCorrectionRequest(
                LocalDate.of(2026, 11, 14), MissingPunchType.CHECK_OUT, null, LocalTime.of(18, 0), "Forgot to check out"
        ), employeePrincipal);

        assertThat(response.status()).isEqualTo(AttendanceCorrectionStatus.PENDING);
        assertThat(response.requestedCheckOutAt()).isEqualTo(Instant.parse("2026-11-14T12:30:00Z"));
        verify(requests).save(any());
    }

    @Test
    void existingCheckInCannotBeRequestedAsMissing() {
        Employee employee = employee();
        when(employees.findByUserId("user-1")).thenReturn(Optional.of(employee));
        when(requests.existsByEmployeeIdAndAttendanceDateAndStatus(anyString(), any(), eq(AttendanceCorrectionStatus.PENDING))).thenReturn(false);
        when(assignments.resolveHistorical(anyString(), any())).thenReturn(assignment());
        when(schedules.load(any(), any())).thenReturn(schedule());
        AttendanceRecord record = new AttendanceRecord();
        AttendanceSession session = new AttendanceSession();
        session.setCheckInAt(Instant.parse("2026-11-14T03:30:00Z"));
        record.getSessions().add(session);
        when(attendance.findByEmployeeIdAndWorkDate(anyString(), any())).thenReturn(Optional.of(record));
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));

        assertThatThrownBy(() -> service.submit(new SubmitAttendanceCorrectionRequest(LocalDate.of(2026, 11, 14), MissingPunchType.CHECK_IN, LocalTime.of(9, 15), null, "Forgot"), employeePrincipal)).hasMessageContaining("already exists");
        verify(requests, never()).save(any());
    }

    @Test
    void duplicatePendingRequestIsPrevented() {
        Employee employee = employee();
        when(employees.findByUserId("user-1")).thenReturn(Optional.of(employee));
        when(requests.existsByEmployeeIdAndAttendanceDateAndStatus(anyString(), any(), eq(AttendanceCorrectionStatus.PENDING))).thenReturn(true);

        assertThatThrownBy(() -> service.submit(new SubmitAttendanceCorrectionRequest(
                LocalDate.of(2026, 11, 14), MissingPunchType.CHECK_OUT, null, LocalTime.of(18, 0), "Duplicate test"
        ), employeePrincipal)).isInstanceOf(ConflictException.class);

        verify(requests, never()).save(any());
    }

    @Test
    void approvedRequestCreatesManualSessionAndRecalculates() {
        Employee employee = employee();
        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));
        AttendanceCorrectionRequestEntity request = new AttendanceCorrectionRequestEntity();
        request.setId("507f1f77bcf86cd799439012");
        request.setEmployeeId(employee.getId());
        request.setAttendanceDate(LocalDate.of(2026, 11, 14));
        request.setMissingPunch(MissingPunchType.BOTH);
        request.setRequestedCheckInAt(Instant.parse("2026-11-14T03:45:00Z"));
        request.setRequestedCheckOutAt(Instant.parse("2026-11-14T12:30:00Z"));
        request.setReason("Forgot both");
        request.setStatus(AttendanceCorrectionStatus.PENDING);
        request.setOriginalAttendanceId(null);
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(attendance.findByEmployeeIdAndWorkDate(anyString(), any())).thenReturn(Optional.empty());
        when(assignments.resolveHistorical(anyString(), any())).thenReturn(assignment());
        when(schedules.load(any(), any())).thenReturn(schedule());
        when(time.now()).thenReturn(Instant.parse("2026-11-15T00:00:00Z"));
        when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.approve(request.getId(), new ReviewAttendanceCorrectionRequest("Approved by HR"), adminPrincipal).status()).isEqualTo(AttendanceCorrectionStatus.APPROVED);
        verify(calculator).recalculate(any(AttendanceRecord.class));
        verify(attendance).save(any(AttendanceRecord.class));
    }

    @Test
    void approvedMissingCheckoutUpdatesIncompleteSessionToCompleted() {
        Employee employee = employee();
        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(time.companyZoneId()).thenReturn(ZoneId.of("Asia/Kolkata"));

        AttendanceRecord record = new AttendanceRecord();
        record.setId("507f1f77bcf86cd799439099");
        record.setEmployeeId(employee.getId());
        record.setWorkDate(LocalDate.of(2026, 11, 14));
        record.setAttendanceStatus(AttendanceStatus.HALF_DAY);

        AttendanceSession session = new AttendanceSession();
        session.setSessionId("sess-1");
        session.setCheckInAt(Instant.parse("2026-11-14T03:30:00Z"));
        session.setStatus(SessionStatus.INCOMPLETE_CHECKOUT);
        record.getSessions().add(session);

        AttendanceCorrectionRequestEntity request = new AttendanceCorrectionRequestEntity();
        request.setId("507f1f77bcf86cd799439013");
        request.setEmployeeId(employee.getId());
        request.setAttendanceDate(LocalDate.of(2026, 11, 14));
        request.setMissingPunch(MissingPunchType.CHECK_OUT);
        request.setRequestedCheckOutAt(Instant.parse("2026-11-14T12:30:00Z"));
        request.setReason("Left at 6pm");
        request.setStatus(AttendanceCorrectionStatus.PENDING);
        request.setOriginalAttendanceId(record.getId());
        request.setOriginalAttendanceVersion(0);

        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate())).thenReturn(Optional.of(record));
        when(assignments.resolveHistorical(anyString(), any())).thenReturn(assignment());
        when(schedules.load(any(), any())).thenReturn(schedule());
        when(time.now()).thenReturn(Instant.parse("2026-11-15T09:00:00Z"));
        when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCorrectionResponse res = service.approve(request.getId(), new ReviewAttendanceCorrectionRequest("Verified with manager"), adminPrincipal);

        assertThat(res.status()).isEqualTo(AttendanceCorrectionStatus.APPROVED);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCheckOutAt()).isEqualTo(Instant.parse("2026-11-14T12:30:00Z"));
        assertThat(session.getClosureType()).isEqualTo("ADMIN_CORRECTION");
        assertThat(session.getCheckOutLocation()).isNull(); // GPS not fabricated
        verify(calculator).recalculate(record);
        verify(attendance).save(record);
    }

    @Test
    void rejectedRequestLeavesOriginalAttendanceUnmodified() {
        Employee employee = employee();
        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));

        AttendanceRecord record = new AttendanceRecord();
        record.setId("rec-halfday");
        record.setWorkDate(LocalDate.of(2026, 11, 14));
        record.setAttendanceStatus(AttendanceStatus.HALF_DAY);

        AttendanceCorrectionRequestEntity request = new AttendanceCorrectionRequestEntity();
        request.setId("507f1f77bcf86cd799439014");
        request.setEmployeeId(employee.getId());
        request.setAttendanceDate(LocalDate.of(2026, 11, 14));
        request.setStatus(AttendanceCorrectionStatus.PENDING);

        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate())).thenReturn(Optional.of(record));
        when(time.now()).thenReturn(Instant.parse("2026-11-15T09:00:00Z"));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceCorrectionResponse res = service.reject(request.getId(), new ReviewAttendanceCorrectionRequest("No proof provided"), adminPrincipal);

        assertThat(res.status()).isEqualTo(AttendanceCorrectionStatus.REJECTED);
        assertThat(request.getStatus()).isEqualTo(AttendanceCorrectionStatus.REJECTED);
        assertThat(record.getAttendanceStatus()).isEqualTo(AttendanceStatus.HALF_DAY);
        verify(attendance, never()).save(any());
        verify(calculator, never()).recalculate(any());
    }

    private Employee employee() {
        Employee e = new Employee();
        e.setId("507f1f77bcf86cd799439011");
        e.setUserId("user-1");
        e.setEmployeeCode("EMP-1");
        e.setEmploymentStatus(EmploymentStatus.ACTIVE);
        e.setJoiningDate(LocalDate.of(2020, 1, 1));
        e.setFirstName("Test");
        e.setLastName("Employee");
        return e;
    }

    private AssignmentResponse assignment() {
        return new AssignmentResponse("a", "507f1f77bcf86cd799439011", "s", "o", LocalDate.of(2020, 1, 1), null, AssignmentStatus.ACTIVE, null, null, null);
    }

    private AttendanceScheduleService.Schedule schedule() {
        ShiftSnapshot shift = new ShiftSnapshot();
        shift.setScheduledStartAt(Instant.parse("2026-11-14T03:30:00Z"));
        shift.setScheduledEndAt(Instant.parse("2026-11-14T12:30:00Z"));
        return new AttendanceScheduleService.Schedule(LocalDate.of(2026, 11, 14), assignment(), shift, null, shift.getScheduledStartAt(), shift.getScheduledEndAt());
    }
}
