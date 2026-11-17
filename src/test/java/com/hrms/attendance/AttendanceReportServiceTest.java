package com.hrms.attendance;

import com.hrms.assignment.model.EmployeeWorkAssignment;
import com.hrms.assignment.repository.AssignmentRepository;
import com.hrms.attendance.dto.AttendanceMonthlyReportResponse;
import com.hrms.attendance.model.AttendanceRecord;
import com.hrms.attendance.model.ShiftSnapshot;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.attendance.service.AttendanceReportService;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.leave.model.LeaveRequest;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.shift.model.Shift;
import com.hrms.shift.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceReportServiceTest {
    @Mock private EmployeeRepository employees;
    @Mock private AssignmentRepository assignments;
    @Mock private ShiftRepository shifts;
    @Mock private AttendanceRepository attendance;
    @Mock private LeaveRequestRepository leaves;
    @Mock private CompanyHolidayRepository holidays;
    @Mock private CompanyTimeProvider time;

    private AttendanceReportService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceReportService(
                employees, assignments, shifts, attendance, leaves, holidays,
                time, ZoneId.of("Asia/Kolkata")
        );
    }

    @Test
    void monthlyReportClassifiesPresentHalfDayAndAbsentUsingHalfShiftThreshold() {
        Employee employee = new Employee();
        employee.setId("employee-1");
        employee.setEmployeeCode("EMP-001");
        employee.setFirstName("Asha");
        employee.setLastName("Sharma");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        employee.setDepartmentId("department-1");

        EmployeeWorkAssignment assignment = new EmployeeWorkAssignment();
        assignment.setEmployeeId(employee.getId());
        assignment.setShiftId("shift-1");
        assignment.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        Shift shift = new Shift();
        shift.setId("shift-1");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setExpectedBreakMinutes(60);
        shift.setWorkingDays(EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ));

        when(employees.findEmployedOnOrBefore(LocalDate.of(2026, 11, 30))).thenReturn(List.of(employee));
        when(assignments.findOverlappingPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(assignment));
        when(shifts.findAllById(Set.of("shift-1"))).thenReturn(List.of(shift));
        CompanyHoliday holiday = new CompanyHoliday();
        holiday.setDate(LocalDate.of(2026, 11, 5));
        holiday.setActive(true);
        when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of(holiday));

        LeaveRequest approvedLeave = new LeaveRequest();
        approvedLeave.setEmployeeId(employee.getId());
        approvedLeave.setLeaveDates(List.of(LocalDate.of(2026, 11, 6)));
        when(leaves.findApprovedOverlappingPeriod(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of(approvedLeave));
        when(time.now()).thenReturn(Instant.parse("2026-12-01T00:00:00Z"));
        when(time.companyDate()).thenReturn(LocalDate.of(2026, 12, 1));

        List<AttendanceRecord> records = List.of(
                record(employee.getId(), LocalDate.of(2026, 11, 1), 0),
                record(employee.getId(), LocalDate.of(2026, 11, 2), 420),
                record(employee.getId(), LocalDate.of(2026, 11, 3), 210),
                record(employee.getId(), LocalDate.of(2026, 11, 4), 100),
                record(employee.getId(), LocalDate.of(2026, 11, 5), 0),
                record(employee.getId(), LocalDate.of(2026, 11, 6), 0)
        );
        when(attendance.findByWorkDateBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(records);

        AttendanceMonthlyReportResponse response = service.monthly("2026-11", null, null);
        AttendanceMonthlyReportResponse.EmployeeRow row = response.employees().getFirst();

        assertThat(row.days().get("2026-11-02").status()).isEqualTo("PRESENT");
        assertThat(row.days().get("2026-11-03").status()).isEqualTo("HALF_DAY");
        assertThat(row.days().get("2026-11-04").status()).isEqualTo("ABSENT");
        assertThat(row.days().get("2026-11-01").status()).isEqualTo("WEEKLY_OFF");
        assertThat(row.days().get("2026-11-05").status()).isEqualTo("HOLIDAY");
        assertThat(row.days().get("2026-11-06").status()).isEqualTo("LEAVE");
        assertThat(row.presentDays()).isEqualTo(1);
        assertThat(row.halfDays()).isEqualTo(1);
        assertThat(row.absentDays()).isEqualTo(17);
        assertThat(response.dates()).hasSize(30);
    }

    @Test
    void incompleteCheckoutSessionReportsAsHalfDayWithZeroWorkedMinutes() {
        Employee employee = new Employee();
        employee.setId("employee-2");
        employee.setEmployeeCode("EMP-002");
        employee.setFirstName("Raj");
        employee.setLastName("Patel");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));

        EmployeeWorkAssignment assignment = new EmployeeWorkAssignment();
        assignment.setEmployeeId(employee.getId());
        assignment.setShiftId("shift-1");
        assignment.setEffectiveFrom(LocalDate.of(2026, 1, 1));

        Shift shift = new Shift();
        shift.setId("shift-1");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setExpectedBreakMinutes(60);
        shift.setWorkingDays(EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ));

        when(employees.findEmployedOnOrBefore(LocalDate.of(2026, 11, 30))).thenReturn(List.of(employee));
        when(assignments.findOverlappingPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(assignment));
        when(shifts.findAllById(Set.of("shift-1"))).thenReturn(List.of(shift));
        when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of());
        when(leaves.findApprovedOverlappingPeriod(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of());
        when(time.now()).thenReturn(Instant.parse("2026-12-01T00:00:00Z"));
        when(time.companyDate()).thenReturn(LocalDate.of(2026, 12, 1));

        AttendanceRecord incRecord = record(employee.getId(), LocalDate.of(2026, 11, 2), 0);
        incRecord.setAttendanceStatus(com.hrms.attendance.model.AttendanceStatus.HALF_DAY);
        incRecord.setOvertimeMinutes(0);
        com.hrms.attendance.model.AttendanceSession s = new com.hrms.attendance.model.AttendanceSession();
        s.setStatus(com.hrms.attendance.model.SessionStatus.INCOMPLETE_CHECKOUT);
        incRecord.getSessions().add(s);

        when(attendance.findByWorkDateBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(incRecord));

        AttendanceMonthlyReportResponse response = service.monthly("2026-11", null, null);
        AttendanceMonthlyReportResponse.EmployeeRow row = response.employees().getFirst();

        assertThat(row.days().get("2026-11-02").status()).isEqualTo("HALF_DAY");
        assertThat(row.days().get("2026-11-02").workedMinutes()).isZero();
        assertThat(row.days().get("2026-11-02").overtimeMinutes()).isZero();
        assertThat(row.halfDays()).isEqualTo(1);
    }

    private AttendanceRecord record(String employeeId, LocalDate date, long workedMinutes) {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(employeeId);
        record.setWorkDate(date);
        record.setTotalWorkedMinutes(workedMinutes);
        record.setOvertimeMinutes(15);
        record.setLateByMinutes(5);

        ShiftSnapshot snapshot = new ShiftSnapshot();
        snapshot.setScheduledStartAt(date.atTime(9, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        snapshot.setScheduledEndAt(date.atTime(17, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
        snapshot.setExpectedBreakMinutes(60);
        snapshot.setWorkingDays(EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ));
        record.setShiftSnapshot(snapshot);
        return record;
    }
}
