package com.hrms.attendance.service;

import com.hrms.assignment.model.AssignmentStatus;
import com.hrms.assignment.model.EmployeeWorkAssignment;
import com.hrms.assignment.repository.AssignmentRepository;
import com.hrms.attendance.dto.AttendanceMonthlyReportResponse;
import com.hrms.attendance.model.AttendanceRecord;
import com.hrms.attendance.model.SessionStatus;
import com.hrms.attendance.model.ShiftSnapshot;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.leave.model.LeaveRequest;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.shift.model.Shift;
import com.hrms.shift.repository.ShiftRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AttendanceReportService {
    private static final String PRESENT = "PRESENT";
    private static final String HALF_DAY = "HALF_DAY";
    private static final String ABSENT = "ABSENT";
    private static final String LEAVE = "LEAVE";
    private static final String HOLIDAY = "HOLIDAY";
    private static final String WEEKLY_OFF = "WEEKLY_OFF";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String UPCOMING = "UPCOMING";
    private static final String UNASSIGNED = "UNASSIGNED";
    private static final String NOT_EMPLOYED = "NOT_EMPLOYED";

    private final EmployeeRepository employees;
    private final AssignmentRepository assignments;
    private final ShiftRepository shifts;
    private final AttendanceRepository attendance;
    private final LeaveRequestRepository leaves;
    private final CompanyHolidayRepository holidays;
    private final CompanyTimeProvider time;
    private final ZoneId companyZone;

    public AttendanceReportService(
            EmployeeRepository employees,
            AssignmentRepository assignments,
            ShiftRepository shifts,
            AttendanceRepository attendance,
            LeaveRequestRepository leaves,
            CompanyHolidayRepository holidays,
            CompanyTimeProvider time,
            ZoneId companyZone
    ) {
        this.employees = employees;
        this.assignments = assignments;
        this.shifts = shifts;
        this.attendance = attendance;
        this.leaves = leaves;
        this.holidays = holidays;
        this.time = time;
        this.companyZone = companyZone;
    }

    public AttendanceMonthlyReportResponse monthly(String month, String departmentId, String employeeCode) {
        YearMonth selectedMonth;
        try {
            selectedMonth = YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "month must use YYYY-MM format"
            );
        }

        LocalDate from = selectedMonth.atDay(1);
        LocalDate to = selectedMonth.atEndOfMonth();
        String normalizedDepartmentId = normalize(departmentId);
        String normalizedEmployeeCode = normalize(employeeCode);
        String employeeCodeSearch = normalizedEmployeeCode == null
                ? null
                : normalizedEmployeeCode.toLowerCase(Locale.ROOT);

        List<Employee> reportEmployees = employees.findEmployedOnOrBefore(to).stream()
                .filter(employee -> employee.getEmploymentStatus() != EmploymentStatus.INACTIVE)
                .filter(employee -> employee.getJoiningDate() == null || !employee.getJoiningDate().isAfter(to))
                .filter(employee -> employee.getTerminationDate() == null || !employee.getTerminationDate().isBefore(from))
                .filter(employee -> normalizedDepartmentId == null
                        || normalizedDepartmentId.equals(employee.getDepartmentId()))
                .filter(employee -> employeeCodeSearch == null
                        || (employee.getEmployeeCode() != null
                        && employee.getEmployeeCode().toLowerCase(Locale.ROOT).contains(employeeCodeSearch)))
                .sorted(Comparator.comparing(Employee::getEmployeeCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<EmployeeWorkAssignment> assignmentRecords = assignments.findOverlappingPeriod(from, to);
        Map<String, List<EmployeeWorkAssignment>> assignmentsByEmployee = new HashMap<>();
        Set<String> shiftIds = new HashSet<>();
        for (EmployeeWorkAssignment assignment : assignmentRecords) {
            assignmentsByEmployee.computeIfAbsent(assignment.getEmployeeId(), ignored -> new ArrayList<>())
                    .add(assignment);
            if (assignment.getShiftId() != null) shiftIds.add(assignment.getShiftId());
        }

        Map<String, Shift> shiftsById = new HashMap<>();
        shifts.findAllById(shiftIds).forEach(shift -> shiftsById.put(shift.getId(), shift));

        Map<String, Map<LocalDate, AttendanceRecord>> attendanceByEmployee = new HashMap<>();
        for (AttendanceRecord record : attendance.findByWorkDateBetween(from, to)) {
            attendanceByEmployee.computeIfAbsent(record.getEmployeeId(), ignored -> new HashMap<>())
                    .put(record.getWorkDate(), record);
        }

        Map<String, Set<LocalDate>> approvedLeaveDays = new HashMap<>();
        for (LeaveRequest leave : leaves.findApprovedOverlappingPeriod(from, to)) {
            if (leave.getLeaveDates() == null) continue;
            for (LocalDate leaveDate : leave.getLeaveDates()) {
                if (!leaveDate.isBefore(from) && !leaveDate.isAfter(to)) {
                    approvedLeaveDays.computeIfAbsent(leave.getEmployeeId(), ignored -> new HashSet<>())
                            .add(leaveDate);
                }
            }
        }

        Set<LocalDate> holidayDates = new HashSet<>();
        for (CompanyHoliday holiday : holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(from, to)) {
            holidayDates.add(holiday.getDate());
        }

        Instant now = time.now();
        LocalDate today = time.companyDate();
        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();

        long totalPresent = 0;
        long totalHalfDays = 0;
        long totalAbsent = 0;
        long totalWorked = 0;
        long totalOvertime = 0;
        long totalLate = 0;
        List<AttendanceMonthlyReportResponse.EmployeeRow> rows = new ArrayList<>();

        for (Employee employee : reportEmployees) {
            Map<String, AttendanceMonthlyReportResponse.DayEntry> dayEntries = new LinkedHashMap<>();
            long presentDays = 0;
            long halfDays = 0;
            long absentDays = 0;
            long workedMinutes = 0;
            long overtimeMinutes = 0;
            long lateMinutes = 0;

            for (LocalDate date : dates) {
                AttendanceMonthlyReportResponse.DayEntry entry = calculateDay(
                        employee,
                        date,
                        today,
                        now,
                        assignmentsByEmployee.getOrDefault(employee.getId(), List.of()),
                        shiftsById,
                        attendanceByEmployee.getOrDefault(employee.getId(), Map.of()).get(date),
                        approvedLeaveDays.getOrDefault(employee.getId(), Set.of()),
                        holidayDates
                );
                dayEntries.put(date.toString(), entry);

                switch (entry.status()) {
                    case PRESENT -> presentDays++;
                    case HALF_DAY -> halfDays++;
                    case ABSENT -> absentDays++;
                    default -> { }
                }
                workedMinutes += entry.workedMinutes();
                overtimeMinutes += entry.overtimeMinutes();
                lateMinutes += entry.lateByMinutes();
            }

            totalPresent += presentDays;
            totalHalfDays += halfDays;
            totalAbsent += absentDays;
            totalWorked += workedMinutes;
            totalOvertime += overtimeMinutes;
            totalLate += lateMinutes;

            String employeeName = ((employee.getFirstName() == null ? "" : employee.getFirstName())
                    + " " + (employee.getLastName() == null ? "" : employee.getLastName())).strip();
            rows.add(new AttendanceMonthlyReportResponse.EmployeeRow(
                    employee.getId(),
                    employee.getEmployeeCode(),
                    employeeName.isBlank() ? employee.getEmployeeCode() : employeeName,
                    employee.getDepartmentId(),
                    dayEntries,
                    presentDays,
                    halfDays,
                    absentDays,
                    workedMinutes,
                    overtimeMinutes,
                    lateMinutes
            ));
        }

        return new AttendanceMonthlyReportResponse(
                selectedMonth.toString(),
                dates,
                rows,
                new AttendanceMonthlyReportResponse.Summary(
                        rows.size(),
                        totalPresent,
                        totalHalfDays,
                        totalAbsent,
                        totalWorked,
                        totalOvertime,
                        totalLate
                )
        );
    }

    private AttendanceMonthlyReportResponse.DayEntry calculateDay(
            Employee employee,
            LocalDate date,
            LocalDate today,
            Instant now,
            List<EmployeeWorkAssignment> employeeAssignments,
            Map<String, Shift> shiftsById,
            AttendanceRecord record,
            Set<LocalDate> leaveDays,
            Set<LocalDate> holidayDates
    ) {
        if ((employee.getJoiningDate() != null && date.isBefore(employee.getJoiningDate()))
                || (employee.getTerminationDate() != null && date.isAfter(employee.getTerminationDate()))) {
            return empty(NOT_EMPLOYED);
        }

        EmployeeWorkAssignment assignment = resolveAssignment(employeeAssignments, date);
        Shift assignedShift = assignment == null ? null : shiftsById.get(assignment.getShiftId());

        if (record != null) {
            ShiftSnapshot snapshot = record.getShiftSnapshot();
            long scheduledMinutes = snapshot != null
                    ? expectedMinutes(snapshot)
                    : expectedMinutes(assignedShift);
            Instant scheduledEnd = snapshot != null && snapshot.getScheduledEndAt() != null
                    ? snapshot.getScheduledEndAt()
                    : scheduledEnd(date, assignedShift);

            boolean openSession = record.getSessions() != null && record.getSessions().stream()
                    .anyMatch(session -> session.getStatus() == SessionStatus.OPEN);
            boolean shiftNotFinished = date.isAfter(today)
                    || (scheduledEnd != null && now.isBefore(scheduledEnd));
            Set<java.time.DayOfWeek> workingDays = snapshot != null && snapshot.getWorkingDays() != null
                    ? snapshot.getWorkingDays()
                    : assignedShift == null ? null : assignedShift.getWorkingDays();
            boolean weeklyOff = workingDays != null && !workingDays.contains(date.getDayOfWeek());
            boolean belowHalfDayThreshold = scheduledMinutes <= 0
                    || record.getTotalWorkedMinutes() * 2 < scheduledMinutes;

            String status;
            if (openSession || shiftNotFinished) {
                status = IN_PROGRESS;
            } else if (belowHalfDayThreshold && holidayDates.contains(date)) {
                status = HOLIDAY;
            } else if (belowHalfDayThreshold && leaveDays.contains(date)) {
                status = LEAVE;
            } else if (belowHalfDayThreshold && weeklyOff) {
                status = WEEKLY_OFF;
            } else {
                status = classify(record.getTotalWorkedMinutes(), scheduledMinutes);
            }

            return new AttendanceMonthlyReportResponse.DayEntry(
                    status,
                    Math.max(0, record.getTotalWorkedMinutes()),
                    Math.max(0, record.getOvertimeMinutes()),
                    Math.max(0, record.getLateByMinutes()),
                    scheduledMinutes
            );
        }

        if (holidayDates.contains(date)) return empty(HOLIDAY);
        if (leaveDays.contains(date)) return empty(LEAVE);

        if (assignment == null || assignedShift == null) return empty(UNASSIGNED);

        Set<java.time.DayOfWeek> workingDays = assignedShift.getWorkingDays();
        if (workingDays == null || !workingDays.contains(date.getDayOfWeek())) {
            return empty(WEEKLY_OFF);
        }

        Instant scheduledEnd = scheduledEnd(date, assignedShift);
        long scheduledMinutes = expectedMinutes(assignedShift);
        if (date.isAfter(today) || scheduledEnd == null || now.isBefore(scheduledEnd)) {
            return new AttendanceMonthlyReportResponse.DayEntry(
                    UPCOMING, 0, 0, 0, scheduledMinutes
            );
        }

        return new AttendanceMonthlyReportResponse.DayEntry(
                ABSENT, 0, 0, 0, scheduledMinutes
        );
    }

    private EmployeeWorkAssignment resolveAssignment(List<EmployeeWorkAssignment> employeeAssignments, LocalDate date) {
        List<EmployeeWorkAssignment> matching = employeeAssignments.stream()
                .filter(assignment -> !(assignment.getStatus() == AssignmentStatus.INACTIVE
                        && assignment.getEffectiveTo() == null))
                .filter(assignment -> assignment.getEffectiveFrom() != null
                        && !date.isBefore(assignment.getEffectiveFrom()))
                .filter(assignment -> assignment.getEffectiveTo() == null
                        || !date.isAfter(assignment.getEffectiveTo()))
                .toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private long expectedMinutes(ShiftSnapshot shift) {
        if (shift == null || shift.getScheduledStartAt() == null || shift.getScheduledEndAt() == null) return 0;
        long duration = Duration.between(shift.getScheduledStartAt(), shift.getScheduledEndAt()).toMinutes();
        return Math.max(0, duration - Math.max(0, shift.getExpectedBreakMinutes()));
    }

    private long expectedMinutes(Shift shift) {
        if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null) return 0;
        long duration = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
        if (duration <= 0) duration += Duration.ofDays(1).toMinutes();
        return Math.max(0, duration - Math.max(0, shift.getExpectedBreakMinutes()));
    }

    private Instant scheduledEnd(LocalDate date, Shift shift) {
        if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null) return null;
        LocalDate endDate = shift.getEndTime().isAfter(shift.getStartTime()) ? date : date.plusDays(1);
        return endDate.atTime(shift.getEndTime()).atZone(companyZone).toInstant();
    }

    private String classify(long workedMinutes, long scheduledMinutes) {
        if (scheduledMinutes <= 0) return UNASSIGNED;
        if (workedMinutes >= scheduledMinutes) return PRESENT;
        if (workedMinutes * 2 >= scheduledMinutes) return HALF_DAY;
        return ABSENT;
    }

    private AttendanceMonthlyReportResponse.DayEntry empty(String status) {
        return new AttendanceMonthlyReportResponse.DayEntry(status, 0, 0, 0, 0);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
