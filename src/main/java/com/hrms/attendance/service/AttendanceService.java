package com.hrms.attendance.service;

import org.springframework.http.HttpStatus;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.attendance.dto.*;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AttendanceService {
    private final AttendanceRepository records;
    private final EmployeeRepository employees;
    private final AttendanceScheduleService schedule;
    private final GeofenceService geofence;
    private final AttendanceCalculationService calculator;
    private final CompanyTimeProvider time;
    private final AuditService audit;
    private final com.hrms.config.AttendanceProperties properties;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public AttendanceService(AttendanceRepository r, EmployeeRepository e, AttendanceScheduleService s,
                             GeofenceService g, AttendanceCalculationService c, CompanyTimeProvider t,
                             AuditService a, com.hrms.config.AttendanceProperties p) {
        records = r;
        employees = e;
        schedule = s;
        geofence = g;
        calculator = c;
        time = t;
        audit = a;
        properties = p;
    }

    @Transactional
    public AttendanceResponse checkIn(AuthPrincipal principal, GpsRequest request) {
        Employee e = employee(principal);
        LocalDate today = time.companyDate();
        closeUnfinishedSessionsForEmployeeBefore(e.getId(), today);

        AttendanceScheduleService.Schedule planned = schedule.resolveCheckIn(e.getId());
        double distance = geofence.validate(request, planned.office());

        synchronized (lock(e.getId() + "/" + planned.workDate())) {
            AttendanceRecord a = records.findByEmployeeIdAndWorkDate(e.getId(), planned.workDate())
                    .orElseGet(() -> newRecord(e, planned));
            if (a.getSessions().stream().anyMatch(s -> s.getStatus() == SessionStatus.OPEN))
                throw new ConflictException("An attendance session is already active");

            Instant now = time.now();
            AttendanceSession session = new AttendanceSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setCheckInAt(now);
            session.setOriginalCheckInAt(now);
            session.setAttendanceDate(planned.workDate());
            session.setClosureType("NORMAL");
            session.setCheckInLocation(new GpsEvidence(request.latitude(), request.longitude(), request.accuracyMeters()));
            session.setCheckInDistanceMeters(distance);
            session.setCheckInGpsAccuracyMeters(request.accuracyMeters());
            session.setStatus(SessionStatus.OPEN);
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            a.getSessions().add(session);
            a.setUpdatedBy(principal.userId());
            calculator.recalculate(a);
            records.save(a);
            audit.record(principal, "ATTENDANCE_CHECK_IN", "ATTENDANCE", a.getId(), Map.of(),
                    Map.of("employeeId", e.getId(), "sessionId", session.getSessionId()), null);
            return AttendanceResponse.from(a);
        }
    }

    @Transactional
    public AttendanceResponse checkOut(AuthPrincipal principal, GpsRequest request) {
        Employee e = employee(principal);
        LocalDate today = time.companyDate();
        closeUnfinishedSessionsForEmployeeBefore(e.getId(), today);

        synchronized (lock(e.getId() + "/open")) {
            OpenSession open = findOpen(e.getId());
            double distance = geofence.validate(request, open.record().getOfficeSnapshot());
            Instant now = time.now();
            AttendanceSession s = open.session();
            if (!now.isAfter(s.getCheckInAt()))
                throw new ApiException(ErrorCode.INVALID_ATTENDANCE_TRANSITION, HttpStatus.BAD_REQUEST, "Checkout must occur after check-in");
            if (Duration.between(s.getCheckInAt(), now).toHours() > properties.maxSessionDurationHours())
                throw new ConflictException("The session exceeds the maximum configured duration and requires review");

            s.setCheckOutAt(now);
            s.setCheckOutLocation(new GpsEvidence(request.latitude(), request.longitude(), request.accuracyMeters()));
            s.setCheckOutDistanceMeters(distance);
            s.setCheckOutGpsAccuracyMeters(request.accuracyMeters());
            s.setStatus(SessionStatus.COMPLETED);
            s.setClosureType("NORMAL");
            s.setClosureReason("Normal employee check-out");
            s.setUpdatedAt(now);
            open.record().setUpdatedBy(principal.userId());
            try {
                calculator.recalculate(open.record());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(ErrorCode.INVALID_ATTENDANCE_TRANSITION, HttpStatus.BAD_REQUEST, "Attendance session transition is invalid");
            }
            records.save(open.record());
            audit.record(principal, "ATTENDANCE_CHECK_OUT", "ATTENDANCE", open.record().getId(), Map.of(),
                    Map.of("employeeId", e.getId(), "sessionId", s.getSessionId()), null);
            return AttendanceResponse.from(open.record());
        }
    }

    public Optional<AttendanceResponse> mine(AuthPrincipal p, LocalDate date) {
        Employee e = employee(p);
        return records.findByEmployeeIdAndWorkDate(e.getId(), date).map(AttendanceResponse::from);
    }

    public AttendancePageResponse history(AuthPrincipal p, LocalDate from, LocalDate to, int page, int limit, String sortBy, String direction) {
        Employee e = employee(p);
        return list(new AttendanceSearch(e.getId(), null, null, null, null, null, from, to, page, limit, sortBy, direction));
    }

    public AttendanceStatusResponse status(AuthPrincipal p) {
        Employee e = employee(p);
        LocalDate today = time.companyDate();
        closeUnfinishedSessionsForEmployeeBefore(e.getId(), today);

        List<AttendanceRecord> list = records.findByEmployeeIdAndWorkDateIn(e.getId(), List.of(today, today.minusDays(1)));
        AttendanceRecord latest = list.stream()
                .filter(a -> a.getWorkDate() != null && !a.getWorkDate().isBefore(today))
                .filter(a -> a.getSessions().stream().anyMatch(s -> s.getStatus() == SessionStatus.OPEN))
                .findFirst()
                .orElse(null);

        AttendanceRecord returnRecord = latest != null ? latest : records.findByEmployeeIdAndWorkDate(e.getId(), today).orElse(null);
        String last = null;
        if (returnRecord != null) {
            last = returnRecord.getSessions().stream()
                    .filter(s -> s.getCheckInAt() != null)
                    .map(s -> s.getCheckInAt().toString())
                    .max(String::compareTo)
                    .orElse(null);
        }
        return new AttendanceStatusResponse(latest != null, returnRecord == null ? null : AttendanceResponse.from(returnRecord), last);
    }

    public AttendancePageResponse list(AttendanceSearch search) {
        validateRange(search.fromDate(), search.toDate());
        if (search.page() < 0 || search.limit() < 1 || search.limit() > 100)
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Page must be non-negative and limit must be between 1 and 100");
        return AttendancePageResponse.from(records.search(search), search.limit());
    }

    public AttendanceResponse detail(String id) {
        return AttendanceResponse.from(find(id));
    }

    public AttendancePageResponse employeeHistory(String employeeId, LocalDate from, LocalDate to, int page, int limit, String sort, String direction) {
        employeeById(employeeId);
        return list(new AttendanceSearch(employeeId, null, null, null, null, null, from, to, page, limit, sort, direction));
    }

    public AttendanceSummaryResponse summary(AttendanceSearch search) {
        validateRange(search.fromDate(), search.toDate());
        Page<AttendanceRecord> page = records.search(new AttendanceSearch(search.employeeId(), search.employeeCode(),
                search.departmentId(), search.officeLocationId(), search.shiftId(), search.status(), search.fromDate(), search.toDate(), 0, 10000, "workDate", "asc"));
        return new AttendanceSummaryResponse(
                page.getTotalElements(),
                page.getContent().stream().filter(a -> a.getAttendanceStatus() == AttendanceStatus.PRESENT).count(),
                page.getContent().stream().filter(a -> a.getAttendanceStatus() == AttendanceStatus.PARTIAL).count(),
                page.getContent().stream().filter(a -> a.getAttendanceStatus() == AttendanceStatus.INCOMPLETE).count(),
                page.getContent().stream().mapToLong(AttendanceRecord::getTotalWorkedMinutes).sum(),
                page.getContent().stream().mapToLong(AttendanceRecord::getLateByMinutes).sum(),
                page.getContent().stream().mapToLong(AttendanceRecord::getEarlyDepartureByMinutes).sum(),
                page.getContent().stream().mapToLong(AttendanceRecord::getOvertimeMinutes).sum()
        );
    }

    @Transactional
    public AttendanceResponse correct(String id, AttendanceCorrectionRequest request, AuthPrincipal admin) {
        AttendanceRecord a = find(id);
        AttendanceSession s = a.getSessions().stream().filter(x -> x.getSessionId().equals(request.sessionId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session was not found"));
        Map<String, Object> before = Map.of("checkInAt", s.getCheckInAt(), "checkOutAt", s.getCheckOutAt(), "status", s.getStatus());
        if (request.correctedCheckOutAt() != null && !request.correctedCheckOutAt().isAfter(request.correctedCheckInAt()))
            throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Corrected checkout must be after check-in");

        Instant now = time.now();
        s.setCheckInAt(request.correctedCheckInAt());
        s.setCheckOutAt(request.correctedCheckOutAt());
        s.setStatus(request.correctedCheckOutAt() == null ? SessionStatus.OPEN : SessionStatus.COMPLETED);
        s.setClosureType("ADMIN_CORRECTION");
        s.setClosureReason("Admin manual correction: " + request.reason().strip());
        s.setUpdatedAt(now);

        CorrectionHistoryEntry h = new CorrectionHistoryEntry();
        h.setCorrectionId(UUID.randomUUID().toString());
        h.setActorId(admin.userId());
        h.setCorrectedAt(now);
        h.setReason(request.reason().strip());
        h.setSessionId(s.getSessionId());
        h.setBefore(before);
        h.setAfter(Map.of("checkInAt", s.getCheckInAt(), "checkOutAt", s.getCheckOutAt(), "status", s.getStatus(), "gpsVerified", false));
        a.getCorrectionHistory().add(h);
        a.setUpdatedBy(admin.userId());
        try {
            calculator.recalculate(a);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_ATTENDANCE_CORRECTION, HttpStatus.BAD_REQUEST, "Correction creates invalid attendance sessions");
        }
        records.save(a);
        audit.record(admin, "ATTENDANCE_CORRECTED", "ATTENDANCE", id, before, h.getAfter(), request.reason());
        return AttendanceResponse.from(a);
    }

    public List<AttendanceRecord> flaggedIncomplete() {
        return records.findBySessionsStatus(SessionStatus.OPEN);
    }

    /**
     * Finds and automatically terminates all open sessions from previous calendar dates.
     * Marks session as INCOMPLETE_CHECKOUT, day as HALF_DAY, worked hours to 0.
     * Idempotent and recoverable across restarts.
     */
    @Transactional
    public int closeUnfinishedSessionsBefore(LocalDate currentDate) {
        List<AttendanceRecord> openRecords = records.findBySessionsStatus(SessionStatus.OPEN);
        int closedCount = 0;
        Instant now = time.now();
        ZoneId companyZone = time.companyZoneId();

        for (AttendanceRecord record : openRecords) {
            if (record.getWorkDate() != null && record.getWorkDate().isBefore(currentDate)) {
                boolean modified = false;
                for (AttendanceSession session : record.getSessions()) {
                    if (session.getStatus() == SessionStatus.OPEN) {
                        closeSessionAsIncomplete(record, session, companyZone, now, "SYSTEM");
                        modified = true;
                        closedCount++;
                    }
                }
                if (modified) {
                    calculator.recalculate(record);
                    record.setUpdatedBy("SYSTEM");
                    records.save(record);
                    audit.record(null, "ATTENDANCE_AUTO_CLOSE_MIDNIGHT", "ATTENDANCE", record.getId(),
                            Map.of("workDate", record.getWorkDate().toString()),
                            Map.of("status", AttendanceStatus.HALF_DAY.name(), "sessionStatus", SessionStatus.INCOMPLETE_CHECKOUT.name()),
                            "Automatically terminated session at midnight due to missing checkout");
                }
            }
        }
        return closedCount;
    }

    /**
     * Defensive check for a specific employee: closes open sessions from dates strictly before currentDate.
     */
    @Transactional
    public int closeUnfinishedSessionsForEmployeeBefore(String employeeId, LocalDate currentDate) {
        List<AttendanceRecord> openRecords = records.findByEmployeeIdAndSessionsStatus(employeeId, SessionStatus.OPEN);
        int closedCount = 0;
        Instant now = time.now();
        ZoneId companyZone = time.companyZoneId();

        for (AttendanceRecord record : openRecords) {
            if (record.getWorkDate() != null && record.getWorkDate().isBefore(currentDate)) {
                boolean modified = false;
                for (AttendanceSession session : record.getSessions()) {
                    if (session.getStatus() == SessionStatus.OPEN) {
                        closeSessionAsIncomplete(record, session, companyZone, now, "SYSTEM");
                        modified = true;
                        closedCount++;
                    }
                }
                if (modified) {
                    calculator.recalculate(record);
                    record.setUpdatedBy("SYSTEM");
                    records.save(record);
                    audit.record(null, "ATTENDANCE_AUTO_CLOSE_MIDNIGHT", "ATTENDANCE", record.getId(),
                            Map.of("workDate", record.getWorkDate().toString()),
                            Map.of("status", AttendanceStatus.HALF_DAY.name(), "sessionStatus", SessionStatus.INCOMPLETE_CHECKOUT.name()),
                            "Automatically terminated session at midnight due to missing checkout");
                }
            }
        }
        return closedCount;
    }

    private void closeSessionAsIncomplete(AttendanceRecord record, AttendanceSession session, ZoneId companyZone, Instant now, String actor) {
        LocalDate workDate = record.getWorkDate();
        Instant endOfDate = workDate.atTime(LocalTime.MAX).atZone(companyZone).toInstant();

        session.setStatus(SessionStatus.INCOMPLETE_CHECKOUT);
        session.setCheckOutAt(endOfDate);
        session.setUpdatedAt(now);
        session.setClosureType("AUTOMATIC_MIDNIGHT");
        session.setClosureReason("Automatically closed at midnight due to missing checkout");
        session.setAutoClosedAt(now);
        session.setOriginalCheckInAt(session.getCheckInAt());
        session.setAttendanceDate(workDate);
        session.setCheckOutLocation(null);
        session.setCheckOutDistanceMeters(0.0);
        session.setCheckOutGpsAccuracyMeters(0.0);
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

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "toDate cannot be earlier than fromDate");
    }

    private Employee employee(AuthPrincipal p) {
        return employeeByUser(p.userId());
    }

    private Employee employeeByUser(String id) {
        return employees.findByUserId(id).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN, "Employee account is inactive or unavailable"));
    }

    private void employeeById(String id) {
        if (!ObjectId.isValid(id))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Employee id is invalid");
        employees.findById(id).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Employee was not found"));
    }

    private AttendanceRecord find(String id) {
        if (!ObjectId.isValid(id))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Attendance id is invalid");
        return records.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attendance record was not found"));
    }

    private OpenSession findOpen(String employeeId) {
        LocalDate today = time.companyDate();
        List<AttendanceRecord> candidates = records.findByEmployeeIdAndWorkDateIn(employeeId, List.of(today, today.minusDays(1)));
        List<OpenSession> opens = new ArrayList<>();
        for (AttendanceRecord a : candidates) {
            // Expired sessions from previous days must never be treated as active sessions
            if (a.getWorkDate().isBefore(today)) {
                continue;
            }
            for (AttendanceSession s : a.getSessions()) {
                if (s.getStatus() == SessionStatus.OPEN) {
                    opens.add(new OpenSession(a, s));
                }
            }
        }
        if (opens.isEmpty())
            throw new ApiException(ErrorCode.NO_ACTIVE_CHECK_IN, HttpStatus.CONFLICT, "There is no active check-in");
        if (opens.size() > 1)
            throw new ApiException(ErrorCode.INVALID_ATTENDANCE_TRANSITION, HttpStatus.CONFLICT, "Multiple active sessions require admin review");
        return opens.getFirst();
    }

    private Object lock(String k) {
        return locks.computeIfAbsent(k, x -> new Object());
    }

    private record OpenSession(AttendanceRecord record, AttendanceSession session) {}
}
