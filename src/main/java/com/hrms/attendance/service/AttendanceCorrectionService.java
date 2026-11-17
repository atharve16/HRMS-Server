package com.hrms.attendance.service;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.attendance.dto.*;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.*;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.config.AttendanceProperties;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.model.PayrollStatus;
import com.hrms.payroll.repository.PayrollRecordRepository;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AttendanceCorrectionService {
    private final AttendanceCorrectionRequestRepository requests;
    private final AttendanceRepository attendance;
    private final EmployeeRepository employees;
    private final AssignmentService assignments;
    private final AttendanceScheduleService schedules;
    private final AttendanceCalculationService calculator;
    private final CompanyTimeProvider time;
    private final AttendanceProperties properties;
    private final AuditService audit;
    private final PayrollRecordRepository payrolls;

    public AttendanceCorrectionService(AttendanceCorrectionRequestRepository requests, AttendanceRepository attendance,
            EmployeeRepository employees, AssignmentService assignments, AttendanceScheduleService schedules,
            AttendanceCalculationService calculator, CompanyTimeProvider time, AttendanceProperties properties, AuditService audit) {
        this(requests, attendance, employees, assignments, schedules, calculator, time, properties, audit, null);
    }

    @Autowired
    public AttendanceCorrectionService(AttendanceCorrectionRequestRepository requests, AttendanceRepository attendance,
            EmployeeRepository employees, AssignmentService assignments, AttendanceScheduleService schedules,
            AttendanceCalculationService calculator, CompanyTimeProvider time, AttendanceProperties properties, AuditService audit,
            PayrollRecordRepository payrolls) {
        this.requests = requests;
        this.attendance = attendance;
        this.employees = employees;
        this.assignments = assignments;
        this.schedules = schedules;
        this.calculator = calculator;
        this.time = time;
        this.properties = properties;
        this.audit = audit;
        this.payrolls = payrolls;
    }

    @Transactional
    public AttendanceCorrectionResponse submit(SubmitAttendanceCorrectionRequest input, AuthPrincipal principal) {
        Employee employee = employeeFor(principal);
        validateEmployment(employee, input.attendanceDate());
        if (requests.existsByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), input.attendanceDate(), AttendanceCorrectionStatus.PENDING))
            throw new ConflictException("A pending attendance correction already exists for this date");
        AttendanceScheduleService.Schedule schedule = schedule(employee.getId(), input.attendanceDate());
        Instant in = instant(input.attendanceDate(), input.requestedCheckInTime());
        Instant out = instant(input.attendanceDate(), input.requestedCheckOutTime());
        validateInput(input, in, out, schedule);
        AttendanceRecord current = attendance.findByEmployeeIdAndWorkDate(employee.getId(), input.attendanceDate()).orElse(null);
        validateMissingPunch(input.missingPunch(), current);
        AttendanceCorrectionRequestEntity request = new AttendanceCorrectionRequestEntity();
        request.setId(new ObjectId().toHexString());
        request.setEmployeeId(employee.getId());
        request.setEmployeeCodeSnapshot(employee.getEmployeeCode());
        request.setEmployeeNameSnapshot((employee.getFirstName() + " " + employee.getLastName()).strip());
        request.setDepartmentIdSnapshot(employee.getDepartmentId());
        request.setAttendanceDate(input.attendanceDate());
        request.setMissingPunch(input.missingPunch());
        request.setRequestedCheckInAt(in);
        request.setRequestedCheckOutAt(out);
        request.setReason(input.reason().strip());
        request.setStatus(AttendanceCorrectionStatus.PENDING);
        request.setOriginalAttendanceId(current == null ? null : current.getId());
        request.setOriginalAttendanceVersion(current == null ? 0 : current.getVersion());
        try {
            requests.save(request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("A pending attendance correction already exists for this date");
        }
        audit.record(principal, "ATTENDANCE_CORRECTION_REQUESTED", "ATTENDANCE_CORRECTION_REQUEST", request.getId(), Map.of(),
                Map.of("employeeId", employee.getId(), "attendanceDate", input.attendanceDate().toString(), "missingPunch", input.missingPunch().name()), request.getReason());
        return response(request, current, schedule.shift());
    }

    public List<AttendanceCorrectionResponse> mine(AuthPrincipal principal) {
        Employee employee = employeeFor(principal);
        return requests.findByEmployeeIdOrderByAttendanceDateDescCreatedAtDesc(employee.getId()).stream().map(r -> response(r,
                attendance.findByEmployeeIdAndWorkDate(employee.getId(), r.getAttendanceDate()).orElse(null), shiftFor(r))).toList();
    }

    public AttendanceCorrectionResponse mineDetail(String id, AuthPrincipal principal) {
        AttendanceCorrectionRequestEntity request = find(id);
        Employee employee = employeeFor(principal);
        if (!request.getEmployeeId().equals(employee.getId())) throw forbidden();
        return response(request, attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate()).orElse(null), shiftFor(request));
    }

    @Transactional
    public AttendanceCorrectionResponse cancel(String id, AuthPrincipal principal) {
        AttendanceCorrectionRequestEntity request = find(id);
        Employee employee = employeeFor(principal);
        if (!request.getEmployeeId().equals(employee.getId())) throw forbidden();
        requirePending(request);
        request.setStatus(AttendanceCorrectionStatus.CANCELLED);
        requests.save(request);
        return response(request, attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate()).orElse(null), shiftFor(request));
    }

    public Page<AttendanceCorrectionResponse> adminList(String employeeId, String departmentId, LocalDate from, LocalDate to,
                                                         AttendanceCorrectionStatus status, int page, int limit) {
        if (page < 0 || limit < 1 || limit > 100)
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Page must be non-negative and limit must be between 1 and 100");
        List<AttendanceCorrectionRequestEntity> filtered = requests.findAll(Sort.by(Sort.Direction.DESC, "attendanceDate", "createdAt")).stream()
                .filter(r -> employeeId == null || employeeId.equals(r.getEmployeeId()))
                .filter(r -> departmentId == null || departmentId.equals(r.getDepartmentIdSnapshot()))
                .filter(r -> from == null || !r.getAttendanceDate().isBefore(from)).filter(r -> to == null || !r.getAttendanceDate().isAfter(to))
                .filter(r -> status == null || status == r.getStatus()).toList();
        int start = Math.min(page * limit, filtered.size());
        int end = Math.min(start + limit, filtered.size());
        List<AttendanceCorrectionResponse> content = filtered.subList(start, end).stream().map(r -> response(r,
                attendance.findByEmployeeIdAndWorkDate(r.getEmployeeId(), r.getAttendanceDate()).orElse(null), shiftFor(r))).toList();
        return new PageImpl<>(content, PageRequest.of(page, limit), filtered.size());
    }

    public AttendanceCorrectionResponse adminDetail(String id) {
        AttendanceCorrectionRequestEntity request = find(id);
        return response(request, attendance.findByEmployeeIdAndWorkDate(request.getEmployeeId(), request.getAttendanceDate()).orElse(null), shiftFor(request));
    }

    @Transactional
    public AttendanceCorrectionResponse approve(String id, ReviewAttendanceCorrectionRequest review, AuthPrincipal admin) {
        AttendanceCorrectionRequestEntity request = find(id);
        requirePending(request);
        Employee employee = employees.findById(request.getEmployeeId()).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee was not found"));
        validateEmployment(employee, request.getAttendanceDate());
        if (payrolls != null) payrolls.findByEmployeeIdAndPayrollMonthAndPayrollYear(employee.getId(), request.getAttendanceDate().getMonthValue(), request.getAttendanceDate().getYear())
                .filter(p -> p.getStatus() == PayrollStatus.FINALIZED || p.getStatus() == PayrollStatus.PAID)
                .ifPresent(p -> { throw new ConflictException("This date belongs to a finalized payroll; use the payroll reconciliation workflow before approving the correction"); });
        AttendanceRecord current = attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate()).orElse(null);
        if (!Objects.equals(request.getOriginalAttendanceId(), current == null ? null : current.getId()) ||
                (current != null && current.getVersion() != request.getOriginalAttendanceVersion()))
            throw new ConflictException("Attendance changed after this request was submitted");
        validateMissingPunch(request.getMissingPunch(), current);
        AttendanceScheduleService.Schedule schedule = schedule(employee.getId(), request.getAttendanceDate());
        applyRequest(request, current, employee, schedule, admin);
        request.setStatus(AttendanceCorrectionStatus.APPROVED);
        request.setAdminResponse(review == null ? null : trim(review.response()));
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(time.now());
        requests.save(request);
        return response(request, attendance.findByEmployeeIdAndWorkDate(employee.getId(), request.getAttendanceDate()).orElse(null), schedule.shift());
    }

    @Transactional
    public AttendanceCorrectionResponse reject(String id, ReviewAttendanceCorrectionRequest review, AuthPrincipal admin) {
        AttendanceCorrectionRequestEntity request = find(id);
        requirePending(request);
        request.setStatus(AttendanceCorrectionStatus.REJECTED);
        request.setAdminResponse(review == null ? null : trim(review.response()));
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(time.now());
        requests.save(request);
        audit.record(admin, "ATTENDANCE_CORRECTION_REJECTED", "ATTENDANCE_CORRECTION_REQUEST", id, Map.of("status", "PENDING"), Map.of("status", "REJECTED"), request.getAdminResponse());
        return response(request, attendance.findByEmployeeIdAndWorkDate(request.getEmployeeId(), request.getAttendanceDate()).orElse(null), shiftFor(request));
    }

    private void applyRequest(AttendanceCorrectionRequestEntity request, AttendanceRecord current, Employee employee,
                              AttendanceScheduleService.Schedule schedule, AuthPrincipal admin) {
        AttendanceRecord record = current == null ? newRecord(employee, schedule) : current;
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("attendanceId", record.getId());
        before.put("sessions", record.getSessions().stream().map(s -> {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("sessionId", s.getSessionId());
            snapshot.put("checkInAt", s.getCheckInAt());
            snapshot.put("checkOutAt", s.getCheckOutAt());
            snapshot.put("status", s.getStatus());
            return snapshot;
        }).toList());

        AttendanceSession session;
        if (request.getMissingPunch() == MissingPunchType.CHECK_OUT) {
            session = record.getSessions().stream()
                    .filter(s -> s.getStatus() == SessionStatus.OPEN || s.getStatus() == SessionStatus.INCOMPLETE_CHECKOUT)
                    .findFirst()
                    .orElseThrow(() -> new ConflictException("There is no eligible session for the missing check-out"));
            if (!request.getRequestedCheckOutAt().isAfter(session.getCheckInAt()))
                throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Corrected checkout must be after check-in");
            LocalDate checkoutDate = request.getRequestedCheckOutAt().atZone(time.companyZoneId()).toLocalDate();
            if (!checkoutDate.equals(request.getAttendanceDate()))
                throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Corrected checkout must belong to the same attendance date");

            session.setCheckOutAt(request.getRequestedCheckOutAt());
            session.setStatus(SessionStatus.COMPLETED);
            session.setClosureType("ADMIN_CORRECTION");
            session.setClosureReason("Admin approved checkout correction: " + (request.getReason() == null ? "" : request.getReason()));
            session.setUpdatedAt(time.now());
            session.setCheckOutLocation(null);
            session.setCheckOutDistanceMeters(0.0);
            session.setCheckOutGpsAccuracyMeters(0.0);
        } else {
            session = record.getSessions().stream().filter(s -> s.getCheckInAt() == null).findFirst().orElse(null);
            if (session == null) {
                session = new AttendanceSession();
                session.setSessionId(UUID.randomUUID().toString());
                session.setCreatedAt(time.now());
                record.getSessions().add(session);
            }
            session.setCheckInAt(request.getRequestedCheckInAt());
            session.setOriginalCheckInAt(request.getRequestedCheckInAt());
            session.setAttendanceDate(request.getAttendanceDate());
            session.setClosureType("ADMIN_CORRECTION");
            session.setClosureReason("Admin approved check-in correction: " + (request.getReason() == null ? "" : request.getReason()));
            session.setCheckInLocation(null);
            session.setCheckInDistanceMeters(0.0);
            session.setCheckInGpsAccuracyMeters(0.0);
            if (request.getMissingPunch() == MissingPunchType.BOTH) {
                if (!request.getRequestedCheckOutAt().isAfter(request.getRequestedCheckInAt()))
                    throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Corrected checkout must be after check-in");
                LocalDate checkoutDate = request.getRequestedCheckOutAt().atZone(time.companyZoneId()).toLocalDate();
                if (!checkoutDate.equals(request.getAttendanceDate()))
                    throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Corrected checkout must belong to the same attendance date");
                session.setCheckOutAt(request.getRequestedCheckOutAt());
                session.setStatus(SessionStatus.COMPLETED);
                session.setCheckOutLocation(null);
                session.setCheckOutDistanceMeters(0.0);
                session.setCheckOutGpsAccuracyMeters(0.0);
            } else {
                session.setStatus(SessionStatus.OPEN);
            }
            session.setUpdatedAt(time.now());
        }

        try {
            calculator.recalculate(record);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Correction creates invalid attendance sessions");
        }
        record.setUpdatedBy(admin.userId());

        CorrectionHistoryEntry history = new CorrectionHistoryEntry();
        history.setCorrectionId(UUID.randomUUID().toString());
        history.setCorrectionRequestId(request.getId());
        history.setActorId(admin.userId());
        history.setSubmittedBy(request.getEmployeeId());
        history.setManual(true);
        history.setCorrectedAt(time.now());
        history.setReason(request.getReason());
        history.setSessionId(session.getSessionId());
        history.setBefore(before);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("attendanceId", record.getId());
        after.put("sessionId", session.getSessionId());
        after.put("checkInAt", session.getCheckInAt());
        after.put("checkOutAt", session.getCheckOutAt());
        after.put("status", session.getStatus());
        after.put("gpsVerified", false);
        history.setAfter(after);
        record.getCorrectionHistory().add(history);

        attendance.save(record);
        audit.record(admin, "ATTENDANCE_CORRECTION_APPROVED", "ATTENDANCE_CORRECTION_REQUEST", request.getId(), before, after, request.getReason());
    }

    private AttendanceRecord newRecord(Employee e, AttendanceScheduleService.Schedule p) {
        AttendanceRecord a = new AttendanceRecord();
        a.setId(new ObjectId().toHexString());
        a.setEmployeeId(e.getId());
        a.setUserId(e.getUserId());
        a.setEmployeeCodeSnapshot(e.getEmployeeCode());
        a.setEmployeeNameSnapshot((e.getFirstName() + " " + e.getLastName()).strip());
        a.setDepartmentIdSnapshot(e.getDepartmentId());
        a.setWorkDate(p.workDate());
        a.setShiftSnapshot(p.shift());
        a.setOfficeSnapshot(p.office());
        a.setAttendanceStatus(AttendanceStatus.INCOMPLETE);
        a.setCreatedBy(e.getUserId());
        a.setUpdatedBy(e.getUserId());
        return a;
    }

    private void validateMissingPunch(MissingPunchType type, AttendanceRecord record) {
        if (record == null) {
            if (type == MissingPunchType.CHECK_OUT) throw new ConflictException("A check-in is required before requesting a check-out");
            return;
        }
        List<AttendanceSession> sessions = record.getSessions();
        if (type == MissingPunchType.BOTH && !sessions.isEmpty())
            throw new ConflictException("Attendance sessions already exist for this date");
        if (type == MissingPunchType.CHECK_IN && sessions.stream().anyMatch(s -> s.getCheckInAt() != null))
            throw new ConflictException("A check-in already exists for this date");
        if (type == MissingPunchType.CHECK_OUT && sessions.stream().filter(s -> s.getStatus() == SessionStatus.OPEN || s.getStatus() == SessionStatus.INCOMPLETE_CHECKOUT).count() != 1)
            throw new ConflictException("There must be exactly one open or incomplete session for a missing check-out");
    }

    private void validateInput(SubmitAttendanceCorrectionRequest input, Instant in, Instant out, AttendanceScheduleService.Schedule schedule) {
        if ((input.missingPunch() == MissingPunchType.CHECK_IN || input.missingPunch() == MissingPunchType.BOTH) && in == null)
            invalid("Requested check-in time is required");
        if ((input.missingPunch() == MissingPunchType.CHECK_OUT || input.missingPunch() == MissingPunchType.BOTH) && out == null)
            invalid("Requested check-out time is required");
        if (in != null && out != null && !in.isBefore(out))
            invalid("Check-in must be earlier than check-out");
        Instant lower = schedule.start().minus(Duration.ofMinutes(properties.earlyCheckInMinutes()));
        Instant upper = schedule.end().plus(Duration.ofMinutes(Math.max(properties.lateCheckInWindowMinutes(), properties.overnightCheckoutGraceMinutes())));
        if (in != null && (in.isBefore(lower) || in.isAfter(upper)))
            invalid("Requested check-in is outside the assigned attendance window");
        if (out != null && (out.isBefore(lower) || out.isAfter(upper)))
            invalid("Requested check-out is outside the assigned attendance window");
    }

    private AttendanceScheduleService.Schedule schedule(String employeeId, LocalDate date) {
        AssignmentResponse assignment = assignments.resolveHistorical(employeeId, date);
        return schedules.load(date, assignment);
    }

    private ShiftSnapshot shiftFor(AttendanceCorrectionRequestEntity r) {
        return attendance.findByEmployeeIdAndWorkDate(r.getEmployeeId(), r.getAttendanceDate()).map(AttendanceRecord::getShiftSnapshot).orElseGet(() -> {
            try {
                return schedule(r.getEmployeeId(), r.getAttendanceDate()).shift();
            } catch (RuntimeException ignored) {
                return null;
            }
        });
    }

    private AttendanceCorrectionResponse response(AttendanceCorrectionRequestEntity r, AttendanceRecord a, ShiftSnapshot shift) {
        return AttendanceCorrectionResponse.from(r, a == null ? null : AttendanceResponse.from(a), shift);
    }

    private Employee employeeFor(AuthPrincipal p) {
        return employees.findByUserId(p.userId()).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN, "Employee account is inactive or unavailable"));
    }

    private void validateEmployment(Employee e, LocalDate date) {
        if (e.getJoiningDate() != null && date.isBefore(e.getJoiningDate()))
            invalid("Attendance date is before the employee joining date");
        if (e.getTerminationDate() != null && date.isAfter(e.getTerminationDate()))
            invalid("Attendance date is after the employee employment period");
    }

    private AttendanceCorrectionRequestEntity find(String id) {
        if (!ObjectId.isValid(id))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Correction request id is invalid");
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attendance correction request was not found"));
    }

    private static void requirePending(AttendanceCorrectionRequestEntity r) {
        if (r.getStatus() != AttendanceCorrectionStatus.PENDING)
            throw new ConflictException("Only pending attendance correction requests can be changed");
    }

    private static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "The correction request does not belong to this employee");
    }

    private static void invalid(String message) {
        throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, message);
    }

    private Instant instant(LocalDate date, LocalTime value) {
        return value == null ? null : date.atTime(value).atZone(time.companyZoneId()).toInstant();
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
