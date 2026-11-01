package com.hrms.leave;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.model.*;
import com.hrms.leave.repository.*;
import com.hrms.leave.service.*;
import com.hrms.notification.service.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {
    private static final String EMPLOYEE_ID = "507f1f77bcf86cd799439011";
    private static final String LEAVE_ID = "507f1f77bcf86cd799439012";
    private final AuthPrincipal employeePrincipal = new AuthPrincipal("user", "employee@example.com", UserRole.EMPLOYEE);
    private final AuthPrincipal otherPrincipal = new AuthPrincipal("other", "other@example.com", UserRole.EMPLOYEE);
    private final AuthPrincipal admin = new AuthPrincipal("admin", "admin@example.com", UserRole.ADMIN);
    @Mock LeaveRequestRepository requests;
    @Mock LeaveBalanceRepository balances;
    @Mock EmployeeRepository employees;
    @Mock LeaveDayCalculationService calculator;
    @Mock CompanyTimeProvider time;
    @Mock AuditService audit;
    @Mock NotificationService notifications;
    LeaveService service;
    Employee employee;

    @BeforeEach
    void setUp() {
        service = new LeaveService(requests, balances, employees, calculator, time, audit, notifications);
        employee = employee(EMPLOYEE_ID, "user");
        lenient().when(employees.findByUserId("user")).thenReturn(Optional.of(employee));
        lenient().when(time.companyDate()).thenReturn(LocalDate.of(2030, 1, 1));
        lenient().when(time.now()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));
        lenient().when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(balances.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void paidApplicationReservesPendingBalance() {
        LocalDate start = LocalDate.of(2030, 1, 7), end = start.plusDays(1);
        LeaveBalance balance = balance(5, 0, 0);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(start), eq(end))).thenReturn(false);
        when(calculator.calculate(EMPLOYEE_ID, start, end)).thenReturn(List.of(start, end));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030)).thenReturn(Optional.of(balance));

        var response = service.apply(new ApplyLeaveRequest(LeaveType.CL, start, end, "Family work"), employeePrincipal);

        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.totalDays()).isEqualByComparingTo("2.0");
        assertThat(balance.getPendingDays()).isEqualByComparingTo("2.0");
        assertThat(balance.getAvailableDays()).isEqualByComparingTo("3.0");
        verify(balances).save(balance);
    }

    @Test
    void paidApplicationReservesOnlyHolidayExcludedDates() {
        LocalDate holiday = LocalDate.of(2030, 1, 7), workingDay = holiday.plusDays(1);
        LeaveBalance balance = balance(5, 0, 0);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(holiday), eq(workingDay))).thenReturn(false);
        when(calculator.calculate(EMPLOYEE_ID, holiday, workingDay)).thenReturn(List.of(workingDay));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030))
                .thenReturn(Optional.of(balance));

        var response = service.apply(new ApplyLeaveRequest(LeaveType.CL, holiday, workingDay, "Family"), employeePrincipal);
        assertThat(response.totalDays()).isEqualByComparingTo("1.0");
        assertThat(balance.getPendingDays()).isEqualByComparingTo("1.0");
        assertThat(balance.getAvailableDays()).isEqualByComparingTo("4.0");
    }

    @Test
    void insufficientPaidBalanceIsRejected() {
        LocalDate date = LocalDate.of(2030, 1, 7);
        LeaveBalance balance = balance(1, 1, 0);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(date), eq(date))).thenReturn(false);
        when(calculator.calculate(EMPLOYEE_ID, date, date)).thenReturn(List.of(date));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.apply(new ApplyLeaveRequest(LeaveType.CL, date, date, "Reason"), employeePrincipal))
                .isInstanceOf(ApiException.class).hasMessageContaining("Insufficient CL balance");
        verify(requests, never()).save(any());
    }

    @Test
    void lwpDoesNotRequireOrReserveBalance() {
        LocalDate date = LocalDate.of(2030, 1, 7);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(date), eq(date))).thenReturn(false);
        when(calculator.calculate(EMPLOYEE_ID, date, date)).thenReturn(List.of(date));

        assertThat(service.apply(new ApplyLeaveRequest(LeaveType.LWP, date, date, "Personal"), employeePrincipal).paid()).isFalse();
        verifyNoInteractions(balances);
    }

    @Test
    void lwpUsesHolidayExcludedDatesFromCalculator() {
        LocalDate holiday = LocalDate.of(2030, 1, 7);
        LocalDate workingDay = holiday.plusDays(1);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(holiday), eq(workingDay))).thenReturn(false);
        when(calculator.calculate(EMPLOYEE_ID, holiday, workingDay)).thenReturn(List.of(workingDay));

        var response = service.apply(new ApplyLeaveRequest(LeaveType.LWP, holiday, workingDay, "Personal"), employeePrincipal);
        assertThat(response.totalDays()).isEqualByComparingTo("1.0");
        verifyNoInteractions(balances);
    }

    @Test
    void overlappingApplicationIsRejectedBeforeBalanceMutation() {
        LocalDate date = LocalDate.of(2030, 1, 7);
        when(requests.existsOverlap(anyString(), anyCollection(), eq(date), eq(date))).thenReturn(true);
        assertThatThrownBy(() -> service.apply(new ApplyLeaveRequest(LeaveType.CL, date, date, "Reason"), employeePrincipal))
                .isInstanceOf(ApiException.class).hasMessageContaining("overlaps");
        verifyNoInteractions(balances);
    }

    @Test
    void approvalMovesPendingDaysToUsedWithoutChangingAvailable() {
        LocalDate date = LocalDate.of(2030, 1, 7);
        LeaveRequest request = pending(LeaveType.CL, List.of(date));
        LeaveBalance balance = balance(5, 1, 1);
        when(requests.findById(LEAVE_ID)).thenReturn(Optional.of(request));
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(calculator.calculate(EMPLOYEE_ID, date, date)).thenReturn(List.of(date));
        when(requests.existsOverlap(EMPLOYEE_ID, Set.of(LeaveStatus.APPROVED), date, date)).thenReturn(false);
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030)).thenReturn(Optional.of(balance));

        assertThat(service.approve(LEAVE_ID, admin).status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(balance.getPendingDays()).isEqualByComparingTo("0.0");
        assertThat(balance.getUsedDays()).isEqualByComparingTo("2.0");
        assertThat(balance.getAvailableDays()).isEqualByComparingTo("3.0");
        verify(notifications).leaveApproved(request);
    }

    @Test
    void rejectionAndCancellationReleaseReservedBalance() {
        LocalDate date = LocalDate.of(2030, 1, 7);
        LeaveRequest rejected = pending(LeaveType.CL, List.of(date));
        LeaveBalance first = balance(5, 1, 1);
        when(requests.findById(LEAVE_ID)).thenReturn(Optional.of(rejected));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030)).thenReturn(Optional.of(first));
        assertThat(service.reject(LEAVE_ID, new RejectLeaveRequest("Not available"), admin).status()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(first.getPendingDays()).isEqualByComparingTo("0.0");
        assertThat(first.getAvailableDays()).isEqualByComparingTo("4.0");
        verify(notifications).leaveRejected(rejected);

        LeaveRequest cancelled = pending(LeaveType.CL, List.of(date));
        LeaveBalance second = balance(5, 1, 1);
        when(requests.findById(LEAVE_ID)).thenReturn(Optional.of(cancelled));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.CL, 2030)).thenReturn(Optional.of(second));
        assertThat(service.cancel(LEAVE_ID, employeePrincipal).status()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(second.getPendingDays()).isEqualByComparingTo("0.0");
        assertThat(second.getAvailableDays()).isEqualByComparingTo("4.0");
    }

    @Test
    void duplicateApprovalAndWrongEmployeeCancellationAreRejected() {
        LeaveRequest approved = pending(LeaveType.CL, List.of(LocalDate.of(2030, 1, 7)));
        approved.setStatus(LeaveStatus.APPROVED);
        when(requests.findById(LEAVE_ID)).thenReturn(Optional.of(approved));
        assertThatThrownBy(() -> service.approve(LEAVE_ID, admin)).isInstanceOf(ApiException.class)
                .hasMessageContaining("Only pending");

        Employee other = employee("507f1f77bcf86cd799439099", "other");
        when(employees.findByUserId("other")).thenReturn(Optional.of(other));
        LeaveRequest pending = pending(LeaveType.CL, List.of(LocalDate.of(2030, 1, 7)));
        when(requests.findById(LEAVE_ID)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.cancel(LEAVE_ID, otherPrincipal)).isInstanceOf(ApiException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void allocationCannotDropBelowCommittedDaysAndLwpCannotBeAllocated() {
        LeaveBalance balance = balance(10, 4, 2);
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(balances.findByEmployeeIdAndLeaveTypeAndYear(EMPLOYEE_ID, LeaveType.EL, 2030)).thenReturn(Optional.of(balance));
        assertThatThrownBy(() -> service.allocate(EMPLOYEE_ID,
                new AllocateLeaveBalanceRequest(LeaveType.EL, 2030, new BigDecimal("5")), admin))
                .isInstanceOf(ApiException.class).hasMessageContaining("used plus pending");
        assertThatThrownBy(() -> service.allocate(EMPLOYEE_ID,
                new AllocateLeaveBalanceRequest(LeaveType.LWP, 2030, BigDecimal.TEN), admin))
                .isInstanceOf(ApiException.class).hasMessageContaining("does not have");
    }

    @Test
    void paginationAndFiltersAreDelegatedToDatabaseSearch() {
        when(requests.search(any())).thenReturn(new PageImpl<>(List.of()));
        service.adminList(new LeaveSearch(EMPLOYEE_ID, "EMP-1", LeaveType.SL, LeaveStatus.PENDING,
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), 1, 10, "startDate", "asc"));
        ArgumentCaptor<LeaveSearch> captor = ArgumentCaptor.forClass(LeaveSearch.class);
        verify(requests).search(captor.capture());
        assertThat(captor.getValue().employeeCode()).isEqualTo("EMP-1");
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().limit()).isEqualTo(10);
    }

    private static Employee employee(String id, String userId) {
        Employee e = new Employee(); e.setId(id); e.setUserId(userId); e.setEmployeeCode("EMP-1");
        e.setEmploymentStatus(EmploymentStatus.ACTIVE); return e;
    }
    private static LeaveBalance balance(int allocated, int used, int pending) {
        LeaveBalance b = new LeaveBalance(); b.setId("507f1f77bcf86cd799439013"); b.setEmployeeId(EMPLOYEE_ID);
        b.setLeaveType(LeaveType.CL); b.setYear(2030); b.setAllocatedDays(BigDecimal.valueOf(allocated));
        b.setUsedDays(BigDecimal.valueOf(used)); b.setPendingDays(BigDecimal.valueOf(pending));
        b.setAvailableDays(BigDecimal.valueOf(allocated - used - pending)); return b;
    }
    private static LeaveRequest pending(LeaveType type, List<LocalDate> dates) {
        LeaveRequest r = new LeaveRequest(); r.setId(LEAVE_ID); r.setEmployeeId(EMPLOYEE_ID); r.setLeaveType(type);
        r.setStartDate(dates.getFirst()); r.setEndDate(dates.getLast()); r.setLeaveDates(dates);
        r.setTotalDays(BigDecimal.valueOf(dates.size())); r.setStatus(LeaveStatus.PENDING); r.setPaid(type.isPaid());
        r.setConflictActive(true); return r;
    }
}
