package com.hrms.leave;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.model.*;
import com.hrms.leave.repository.*;
import com.hrms.leave.service.LeaveAllocationService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LeaveAllocationServiceTest {
    private final LeavePeriodBalanceRepository balances = mock(LeavePeriodBalanceRepository.class);
    private final LeaveLedgerEntryRepository ledger = mock(LeaveLedgerEntryRepository.class);
    private final LeavePolicyRepository policies = mock(LeavePolicyRepository.class);
    private final LeaveRolloverRecordRepository rollovers = mock(LeaveRolloverRecordRepository.class);
    private final LeaveRequestRepository requests = mock(LeaveRequestRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final CompanyTimeProvider time = mock(CompanyTimeProvider.class);
    private final LeaveAllocationService service = new LeaveAllocationService(balances, ledger, policies, rollovers, requests, employees, time, mock(AuditService.class), mock(PlatformTransactionManager.class));
    private final AuthPrincipal admin = new AuthPrincipal("admin", "admin@example.com", UserRole.ADMIN);

    @Test void manualAdjustmentIsRecordedWithoutOverwritingUsage() {
        Employee e = employee(); LeavePeriodBalance balance = balance("2026-11"); when(employees.findById(e.getId())).thenReturn(Optional.of(e)); when(balances.findByEmployeeIdAndLeaveTypeAndPeriod(e.getId(), LeaveType.CL, "2026-11")).thenReturn(Optional.of(balance)); when(balances.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.allocate(e.getId(), new LeaveAllocationRequest(LeaveType.CL, "2026-11", new BigDecimal("1.5"), "Quarterly adjustment"), admin);
        assertThat(result.allocatedDays()).isEqualByComparingTo("3.5"); assertThat(result.usedDays()).isEqualByComparingTo("1"); verify(ledger).save(any(LeaveLedgerEntry.class));
    }

    @Test void rolloverSkipsAnAlreadyProcessedPeriod() {
        Employee e = employee(); LeaveRolloverRecord record = new LeaveRolloverRecord(); when(employees.findByEmploymentStatus(EmploymentStatus.ACTIVE)).thenReturn(List.of(e)); when(rollovers.findByEmployeeIdAndPeriod(e.getId(), "2026-10")).thenReturn(Optional.of(record));
        service.rollover(YearMonth.of(2026, 10));
        verify(balances, never()).save(any());
    }

    private Employee employee() { Employee e = new Employee(); e.setId("507f1f77bcf86cd799439011"); e.setEmploymentStatus(EmploymentStatus.ACTIVE); return e; }
    private LeavePeriodBalance balance(String period) { LeavePeriodBalance b = new LeavePeriodBalance(); b.setId("b"); b.setEmployeeId("507f1f77bcf86cd799439011"); b.setLeaveType(LeaveType.CL); b.setPeriod(period); b.setAllocatedDays(new BigDecimal("2")); b.setUsedDays(BigDecimal.ONE); b.setPendingDays(BigDecimal.ZERO); b.setAvailableDays(BigDecimal.ONE); return b; }
}
