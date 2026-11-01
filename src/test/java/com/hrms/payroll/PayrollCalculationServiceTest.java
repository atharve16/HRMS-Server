package com.hrms.payroll;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.model.AssignmentStatus;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.department.repository.DepartmentRepository;
import com.hrms.employee.model.Employee;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.leave.model.LeaveType;
import com.hrms.leave.service.LeaveService;
import com.hrms.payroll.model.*;
import com.hrms.payroll.service.PayrollCalculationService;
import com.hrms.shift.model.Shift;
import com.hrms.shift.repository.ShiftRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PayrollCalculationServiceTest {
    AssignmentService assignments=mock(AssignmentService.class); ShiftRepository shifts=mock(ShiftRepository.class);
    CompanyHolidayRepository holidays=mock(CompanyHolidayRepository.class); AttendanceRepository attendance=mock(AttendanceRepository.class);
    LeaveService leaves=mock(LeaveService.class); DepartmentRepository departments=mock(DepartmentRepository.class);
    PayrollCalculationService service=new PayrollCalculationService(assignments,shifts,holidays,attendance,leaves,departments);
    Employee employee; SalaryStructure salary; String shiftId=new ObjectId().toHexString();

    @BeforeEach void setup(){employee=new Employee();employee.setId(new ObjectId().toHexString());employee.setEmployeeCode("EMP-1");
        employee.setFirstName("Ada");employee.setLastName("Lovelace");employee.setJoiningDate(LocalDate.of(2026,11,2));
        salary=new SalaryStructure();salary.setMonthlyFixedSalary(new BigDecimal("4000"));
        Shift shift=new Shift();shift.setId(shiftId);shift.setWorkingDays(EnumSet.range(DayOfWeek.MONDAY,DayOfWeek.FRIDAY));
        when(assignments.resolveHistorical(eq(employee.getId()),any())).thenAnswer(i->new AssignmentResponse("a",employee.getId(),shiftId,"o",
                LocalDate.of(2026,1,1),null,AssignmentStatus.ACTIVE,null,null,null));
        when(shifts.findById(shiftId)).thenReturn(Optional.of(shift));when(departments.findById(anyString())).thenReturn(Optional.empty());
        when(attendance.findByEmployeeIdAndWorkDateBetween(anyString(),any(),any())).thenReturn(List.of());
        when(leaves.approvedLeaveTypes(anyString(),any(),any())).thenReturn(Map.of());when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(any(),any())).thenReturn(List.of());}

    @Test void calculatesScheduleHolidayPaidLeaveLwpAndAbsenceWithoutDoubleCounting(){
        employee.setTerminationDate(LocalDate.of(2026,11,6));CompanyHoliday holiday=new CompanyHoliday();holiday.setDate(LocalDate.of(2026,11,3));
        when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(any(),any())).thenReturn(List.of(holiday));
        when(attendance.findByEmployeeIdAndWorkDateBetween(anyString(),any(),any())).thenReturn(List.of(completed(LocalDate.of(2026,11,2))));
        when(leaves.approvedLeaveTypes(anyString(),any(),any())).thenReturn(Map.of(LocalDate.of(2026,11,4),LeaveType.LWP,
                LocalDate.of(2026,11,5),LeaveType.CL,LocalDate.of(2026,11,2),LeaveType.LWP));
        PayrollRecord result=service.calculate(employee,salary,11,2026,"admin");
        assertThat(result.getScheduledWorkingDays()).isEqualTo(4);assertThat(result.getAttendedDays()).isEqualTo(1);
        assertThat(result.getPaidLeaveDays()).isEqualTo(1);assertThat(result.getUnpaidLeaveDays()).isEqualTo(1);
        assertThat(result.getUnexcusedAbsenceDays()).isEqualTo(1);assertThat(result.getTotalUnpaidDays()).isEqualTo(2);
        assertThat(result.getDailySalaryRate()).isEqualByComparingTo("1000.00");assertThat(result.getSalaryDeduction()).isEqualByComparingTo("2000.00");
        assertThat(result.getNetSalary()).isEqualByComparingTo("2000.00");assertThat(result.getCalculationDetails().getHolidayDates()).containsExactly(LocalDate.of(2026,11,3));
    }

    @Test void honorsJoiningTerminationWeeklyOffAndEffectiveAssignmentPerDate(){employee.setJoiningDate(LocalDate.of(2026,11,6));employee.setTerminationDate(LocalDate.of(2026,11,9));
        when(attendance.findByEmployeeIdAndWorkDateBetween(anyString(),any(),any())).thenReturn(List.of(completed(LocalDate.of(2026,11,6)),completed(LocalDate.of(2026,11,9))));
        PayrollRecord result=service.calculate(employee,salary,11,2026,"admin");
        assertThat(result.getScheduledWorkingDays()).isEqualTo(2);assertThat(result.getAttendedDays()).isEqualTo(2);
        assertThat(result.getCalculationDetails().getWeeklyOffDates()).containsExactly(LocalDate.of(2026,11,7),LocalDate.of(2026,11,8));
        verify(assignments,times(4)).resolveHistorical(eq(employee.getId()),any());
    }

    @Test void blocksIncompleteAttendance(){employee.setTerminationDate(LocalDate.of(2026,11,2));AttendanceRecord record=new AttendanceRecord();
        record.setWorkDate(LocalDate.of(2026,11,2));record.setAttendanceStatus(AttendanceStatus.INCOMPLETE);AttendanceSession open=new AttendanceSession();open.setStatus(SessionStatus.OPEN);open.setCheckInAt(Instant.now());record.setSessions(List.of(open));
        when(attendance.findByEmployeeIdAndWorkDateBetween(anyString(),any(),any())).thenReturn(List.of(record));
        assertThatThrownBy(()->service.calculate(employee,salary,11,2026,"admin")).isInstanceOf(ApiException.class).hasMessageContaining("Unresolved attendance");
    }

    private AttendanceRecord completed(LocalDate date){AttendanceSession session=new AttendanceSession();session.setStatus(SessionStatus.COMPLETED);
        session.setCheckInAt(date.atStartOfDay(ZoneOffset.UTC).toInstant());session.setCheckOutAt(session.getCheckInAt().plusSeconds(8*3600));
        AttendanceRecord record=new AttendanceRecord();record.setWorkDate(date);record.setAttendanceStatus(AttendanceStatus.PARTIAL);record.setSessions(List.of(session));return record;}
}
