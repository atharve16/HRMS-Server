package com.hrms.leave.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.model.*;
import com.hrms.leave.repository.*;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
public class LeaveAllocationService {
    private final LeavePeriodBalanceRepository balances;
    private final LeaveLedgerEntryRepository ledger;
    private final LeavePolicyRepository policies;
    private final LeaveRolloverRecordRepository rollovers;
    private final LeaveRequestRepository requests;
    private final EmployeeRepository employees;
    private final CompanyTimeProvider time;
    private final AuditService audit;
    private final TransactionTemplate transactions;

    public LeaveAllocationService(LeavePeriodBalanceRepository balances, LeaveLedgerEntryRepository ledger,
                                  LeavePolicyRepository policies, LeaveRolloverRecordRepository rollovers,
                                  LeaveRequestRepository requests, EmployeeRepository employees,
                                  CompanyTimeProvider time, AuditService audit, PlatformTransactionManager transactionManager) {
        this.balances = balances; this.ledger = ledger; this.policies = policies; this.rollovers = rollovers;
        this.requests = requests; this.employees = employees; this.time = time; this.audit = audit;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public LeavePeriodBalanceResponse allocate(String employeeId, LeaveAllocationRequest input, AuthPrincipal admin) {
        employee(employeeId);
        paid(input.leaveType());
        YearMonth.parse(input.month());
        LeavePeriodBalance balance = ensure(employeeId, input.leaveType(), input.month());
        BigDecimal delta = normalize(input.days());
        BigDecimal before = total(balance);
        BigDecimal nextAllocated = balance.getAllocatedDays().add(delta);
        if (nextAllocated.signum() < 0 || nextAllocated.compareTo(balance.getUsedDays().add(balance.getPendingDays())) < 0)
            throw new ApiException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE, HttpStatus.CONFLICT,
                    "Allocation cannot be below used plus pending days");
        balance.setAllocatedDays(nextAllocated);
        recalculate(balance);
        balances.save(balance);
        entry(employeeId, input.leaveType(), input.month(), delta.signum() < 0 ? LeaveLedgerEntryType.ADJUSTMENT : LeaveLedgerEntryType.ALLOCATION,
                delta, before, total(balance), admin.userId(), input.reason().strip(), "ADMIN_MANUAL", null);
        audit.record(admin, "LEAVE_PERIOD_ALLOCATION", "LEAVE_PERIOD_BALANCE", balance.getId(),
                Map.of("allocatedDays", before), Map.of("employeeId", employeeId, "leaveType", input.leaveType().name(),
                        "period", input.month(), "delta", delta), input.reason());
        return LeavePeriodBalanceResponse.from(balance);
    }

    public List<LeavePeriodBalanceResponse> employeeBalances(String employeeId, String month) {
        employee(employeeId); YearMonth.parse(month);
        return Arrays.stream(LeaveType.values()).filter(LeaveType::isPaid)
                .map(type -> ensure(employeeId, type, month)).map(LeavePeriodBalanceResponse::from).toList();
    }

    public List<LeavePeriodBalanceResponse> employeeBalancesByPrincipal(AuthPrincipal principal, String month) {
        return employeeBalances(employees.findByUserId(principal.userId())
                .filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN, "Employee account is inactive or unavailable"))
                .getId(), month);
    }

    public List<LeaveLedgerEntryResponse> history(String employeeId, String month) {
        employee(employeeId); YearMonth.parse(month);
        return ledger.findByEmployeeIdAndPeriodOrderByCreatedAtDesc(employeeId, month).stream().map(LeaveLedgerEntryResponse::from).toList();
    }

    @Transactional
    public LeavePolicyResponse saveCompanyPolicy(LeavePolicyRequest input, AuthPrincipal admin) {
        paid(input.leaveType());
        LeavePolicy policy = policies.findByScopeAndEmployeeIdAndLeaveType(LeavePolicyScope.COMPANY, null, input.leaveType())
                .orElseGet(() -> newPolicy(LeavePolicyScope.COMPANY, null, input.leaveType()));
        policy.setMonthlyEntitlement(normalize(input.monthlyEntitlement()));
        policy.setMaxCarryForwardDays(input.maxCarryForwardDays() == null ? null : normalize(input.maxCarryForwardDays()));
        policy.setUpdatedBy(admin.userId()); policies.save(policy);
        audit.record(admin, "LEAVE_POLICY_UPDATED", "LEAVE_POLICY", policy.getId(), Map.of(),
                Map.of("scope", "COMPANY", "leaveType", input.leaveType().name()), null);
        return LeavePolicyResponse.from(policy);
    }

    @Transactional
    public LeavePolicyResponse saveEmployeePolicy(String employeeId, LeavePolicyRequest input, AuthPrincipal admin) {
        employee(employeeId); paid(input.leaveType());
        LeavePolicy policy = policies.findByScopeAndEmployeeIdAndLeaveType(LeavePolicyScope.EMPLOYEE, employeeId, input.leaveType())
                .orElseGet(() -> newPolicy(LeavePolicyScope.EMPLOYEE, employeeId, input.leaveType()));
        policy.setMonthlyEntitlement(normalize(input.monthlyEntitlement()));
        policy.setMaxCarryForwardDays(input.maxCarryForwardDays() == null ? null : normalize(input.maxCarryForwardDays()));
        policy.setUpdatedBy(admin.userId()); policies.save(policy);
        return LeavePolicyResponse.from(policy);
    }

    public List<LeavePolicyResponse> policies(String employeeId) {
        List<LeavePolicy> result = new ArrayList<>(policies.findByScope(LeavePolicyScope.COMPANY));
        if (employeeId != null) result.addAll(policies.findByScopeAndEmployeeId(LeavePolicyScope.EMPLOYEE, employeeId));
        return result.stream().map(LeavePolicyResponse::from).toList();
    }

    public LeaveDashboardResponse dashboard(String month) {
        YearMonth.parse(month);
        List<LeavePeriodBalance> current = balances.findByPeriod(month);
        BigDecimal allocated = current.stream().map(LeaveAllocationService::totalAllocated).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumed = current.stream().map(LeavePeriodBalance::getUsedDays).reduce(BigDecimal.ZERO, BigDecimal::add);
        long low = current.stream().filter(b -> b.getAvailableDays().compareTo(BigDecimal.ONE) < 0).map(LeavePeriodBalance::getEmployeeId).distinct().count();
        long pending = requests.countByStatus(LeaveStatus.PENDING);
        return new LeaveDashboardResponse(month, allocated, consumed, pending, low,
                current.stream().map(LeavePeriodBalanceResponse::from).toList());
    }

    /** Retries safely on startup and hourly; the unique rollover key makes it idempotent. */
    @Scheduled(fixedDelayString = "${LEAVE_ROLLOVER_INTERVAL_MS:3600000}")
    public void scheduledRollover() {
        try { transactions.executeWithoutResult(status -> rollover(YearMonth.from(time.companyDate()).minusMonths(1))); }
        catch (RuntimeException exception) { /* application logging is intentionally delegated to the scheduler logger */
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Leave rollover failed", exception);
        }
    }

    @Transactional
    public void rollover(YearMonth period) {
        String source = period.toString(); String target = period.plusMonths(1).toString();
        for (Employee employee : employees.findByEmploymentStatus(EmploymentStatus.ACTIVE)) {
            try {
                if (rollovers.findByEmployeeIdAndPeriod(employee.getId(), source).isPresent()) continue;
                for (LeaveType type : Arrays.stream(LeaveType.values()).filter(LeaveType::isPaid).toList()) {
                    LeavePeriodBalance old = ensure(employee.getId(), type, source);
                    LeavePolicy policy = effectivePolicy(employee.getId(), type);
                    BigDecimal available = old.getAvailableDays();
                    BigDecimal carry = type == LeaveType.EL ? limit(available, policy == null ? null : policy.getMaxCarryForwardDays()) : BigDecimal.ZERO;
                    BigDecimal expired = available.subtract(carry);
                    old.setExpiredDays(old.getExpiredDays().add(expired));
                    recalculate(old); balances.save(old);
                    LeavePeriodBalance next = ensure(employee.getId(), type, target);
                    if (carry.signum() > 0) {
                        BigDecimal before = total(next); next.setCarriedForwardDays(next.getCarriedForwardDays().add(carry));
                        recalculate(next); balances.save(next);
                        entry(employee.getId(), type, target, LeaveLedgerEntryType.CARRY_FORWARD, carry, before, total(next), "SYSTEM", "Monthly EL rollover", "MONTHLY_ROLLOVER", source);
                    }
                    if (expired.signum() > 0) entry(employee.getId(), type, source, LeaveLedgerEntryType.EXPIRY, expired,
                            available, old.getAvailableDays(), "SYSTEM", "Month-end expiry", "MONTHLY_ROLLOVER", source + ":expiry:" + type.name());
                }
                LeaveRolloverRecord record = new LeaveRolloverRecord(); record.setId(new ObjectId().toHexString());
                record.setEmployeeId(employee.getId()); record.setPeriod(source); record.setProcessedAt(time.now()); record.setStatus("COMPLETED");
                try { rollovers.save(record); } catch (DuplicateKeyException ignored) { /* another scheduler instance won */ }
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger(getClass()).error("Leave rollover failed for employee {} and period {}", employee.getId(), source, exception);
            }
        }
    }

    private LeavePeriodBalance ensure(String employeeId, LeaveType type, String month) {
        return balances.findByEmployeeIdAndLeaveTypeAndPeriod(employeeId, type, month).orElseGet(() -> {
            LeavePeriodBalance b = new LeavePeriodBalance(); b.setId(new ObjectId().toHexString()); b.setEmployeeId(employeeId); b.setLeaveType(type); b.setPeriod(month);
            LeavePolicy policy = effectivePolicy(employeeId, type);
            b.setAllocatedDays(policy == null ? BigDecimal.ZERO : normalize(policy.getMonthlyEntitlement()));
            recalculate(b);
            try { return balances.save(b); } catch (DuplicateKeyException ex) { return balances.findByEmployeeIdAndLeaveTypeAndPeriod(employeeId, type, month).orElseThrow(); }
        });
    }

    private LeavePolicy effectivePolicy(String employeeId, LeaveType type) {
        return policies.findByScopeAndEmployeeIdAndLeaveType(LeavePolicyScope.EMPLOYEE, employeeId, type)
                .or(() -> policies.findByScopeAndEmployeeIdAndLeaveType(LeavePolicyScope.COMPANY, null, type)).orElse(null);
    }
    private LeavePolicy newPolicy(LeavePolicyScope scope, String employeeId, LeaveType type) { LeavePolicy p = new LeavePolicy(); p.setId(new ObjectId().toHexString()); p.setScope(scope); p.setEmployeeId(employeeId); p.setLeaveType(type); return p; }
    private Employee employee(String id) { if (!ObjectId.isValid(id)) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Employee id is invalid"); return employees.findById(id).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("Active employee was not found")); }
    private static void paid(LeaveType type) { if (!type.isPaid()) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "LWP does not have an allocated balance"); }
    private void entry(String employeeId, LeaveType type, String period, LeaveLedgerEntryType kind, BigDecimal days, BigDecimal before, BigDecimal after, String actor, String reason, String source, String key) { LeaveLedgerEntry e = new LeaveLedgerEntry(); e.setId(new ObjectId().toHexString()); e.setEmployeeId(employeeId); e.setLeaveType(type); e.setPeriod(period); e.setEntryType(kind); e.setDays(days); e.setBalanceBefore(before); e.setBalanceAfter(after); e.setActorId(actor); e.setReason(reason); e.setSource(source); e.setIdempotencyKey(key); try { ledger.save(e); } catch (DuplicateKeyException ignored) { } }
    private static BigDecimal total(LeavePeriodBalance b) { return b.getAvailableDays().add(b.getUsedDays()).add(b.getPendingDays()); }
    private static BigDecimal totalAllocated(LeavePeriodBalance b) { return b.getAllocatedDays().add(b.getCarriedForwardDays()); }
    private static BigDecimal limit(BigDecimal value, BigDecimal max) { return max == null ? value : value.min(max); }
    private static BigDecimal normalize(BigDecimal value) { return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value; }
    private static void recalculate(LeavePeriodBalance b) { BigDecimal available = b.getAllocatedDays().add(b.getCarriedForwardDays()).subtract(b.getExpiredDays()).subtract(b.getUsedDays()).subtract(b.getPendingDays()); if (available.signum() < 0) throw new ApiException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE, HttpStatus.CONFLICT, "Leave balance cannot be negative"); b.setAvailableDays(available); }
}
