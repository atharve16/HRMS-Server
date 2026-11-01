package com.hrms.leave;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.model.AssignmentStatus;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.common.exceptions.ApiException;
import com.hrms.leave.service.LeaveDayCalculationService;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.shift.model.*;
import com.hrms.shift.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LeaveDayCalculationServiceTest {
    @Mock AssignmentService assignments;
    @Mock ShiftRepository shifts;
    @Mock CompanyHolidayRepository holidays;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void excludesWeeklyOffsFromInclusiveRange() {
        LeaveDayCalculationService service = service();
        Shift weekday = shift("weekday", EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        stubRange(LocalDate.of(2030, 1, 4), LocalDate.of(2030, 1, 7), "weekday");
        when(shifts.findById("weekday")).thenReturn(Optional.of(weekday));

        assertThat(service.calculate(employeeId(), LocalDate.of(2030, 1, 4), LocalDate.of(2030, 1, 7)))
                .containsExactly(LocalDate.of(2030, 1, 4), LocalDate.of(2030, 1, 7));
    }

    @Test
    void usesDifferentEmployeeShiftSchedules() {
        LeaveDayCalculationService service = service();
        LocalDate monday = LocalDate.of(2030, 1, 7);
        when(assignments.resolve("employee-a", monday)).thenReturn(assignment("employee-a", "mon"));
        when(assignments.resolve("employee-b", monday)).thenReturn(assignment("employee-b", "tue"));
        when(shifts.findById("mon")).thenReturn(Optional.of(shift("mon", EnumSet.of(DayOfWeek.MONDAY))));
        when(shifts.findById("tue")).thenReturn(Optional.of(shift("tue", EnumSet.of(DayOfWeek.TUESDAY))));

        assertThat(service.calculate("employee-a", monday, monday)).containsExactly(monday);
        assertThatThrownBy(() -> service.calculate("employee-b", monday, monday)).isInstanceOf(ApiException.class)
                .hasMessageContaining("no scheduled working days");
    }

    @Test
    void resolvesAssignmentChangesForEveryDate() {
        LeaveDayCalculationService service = service();
        LocalDate monday = LocalDate.of(2030, 1, 7);
        LocalDate tuesday = monday.plusDays(1);
        when(assignments.resolve(employeeId(), monday)).thenReturn(assignment(employeeId(), "monday"));
        when(assignments.resolve(employeeId(), tuesday)).thenReturn(assignment(employeeId(), "tuesday"));
        when(shifts.findById("monday")).thenReturn(Optional.of(shift("monday", EnumSet.of(DayOfWeek.MONDAY))));
        when(shifts.findById("tuesday")).thenReturn(Optional.of(shift("tuesday", EnumSet.of(DayOfWeek.TUESDAY))));

        assertThat(service.calculate(employeeId(), monday, tuesday)).containsExactly(monday, tuesday);
    }

    @Test
    void rejectsInvalidRangeAndMissingAssignment() {
        LeaveDayCalculationService service = service();
        LocalDate date = LocalDate.of(2030, 1, 7);
        assertThatThrownBy(() -> service.calculate(employeeId(), date, date.minusDays(1)))
                .isInstanceOf(ApiException.class).hasMessageContaining("cannot be earlier");
        when(assignments.resolve(employeeId(), date)).thenThrow(new ApiException(
                com.hrms.common.exceptions.ErrorCode.CONFLICT, org.springframework.http.HttpStatus.CONFLICT, "missing"));
        assertThatThrownBy(() -> service.calculate(employeeId(), date, date))
                .isInstanceOf(ApiException.class).hasMessageContaining(date.toString());
    }

    @Test
    void excludesActiveHolidayAndDoesNotDoubleCountHolidayOnWeeklyOff() {
        LeaveDayCalculationService service = service();
        LocalDate monday = LocalDate.of(2030, 1, 7);
        LocalDate saturday = monday.plusDays(5);
        stubRange(monday, saturday, "weekday");
        when(shifts.findById("weekday")).thenReturn(Optional.of(
                shift("weekday", EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))));
        when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(monday, saturday))
                .thenReturn(List.of(holiday(monday), holiday(saturday)));

        assertThat(service.calculate(employeeId(), monday, saturday))
                .containsExactly(monday.plusDays(1), monday.plusDays(2), monday.plusDays(3), monday.plusDays(4));
    }

    private void stubRange(LocalDate from, LocalDate to, String shiftId) {
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))
            when(assignments.resolve(employeeId(), date)).thenReturn(assignment(employeeId(), shiftId));
    }
    private static AssignmentResponse assignment(String employeeId, String shiftId) {
        return new AssignmentResponse("assignment", employeeId, shiftId, "office", LocalDate.of(2020, 1, 1),
                null, AssignmentStatus.ACTIVE, null, null, null);
    }
    private static Shift shift(String id, Set<DayOfWeek> days) {
        Shift shift = new Shift(); shift.setId(id); shift.setWorkingDays(days); shift.setStatus(ShiftStatus.ACTIVE); return shift;
    }
    private LeaveDayCalculationService service() { return new LeaveDayCalculationService(assignments, shifts, holidays); }
    private static CompanyHoliday holiday(LocalDate date) {
        CompanyHoliday holiday = new CompanyHoliday(); holiday.setDate(date); holiday.setActive(true); return holiday;
    }
    private static String employeeId() { return "507f1f77bcf86cd799439011"; }
}
