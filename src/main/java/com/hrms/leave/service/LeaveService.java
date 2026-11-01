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
import com.hrms.notification.service.NotificationService;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaveService {
    private static final Set<LeaveStatus> BLOCKING_STATUSES = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private final LeaveRequestRepository requests;
    private final LeaveBalanceRepository balances;
    private final EmployeeRepository employees;
    private final LeaveDayCalculationService calculator;
    private final CompanyTimeProvider time;
    private final AuditService audit;
    private final NotificationService notifications;

    public LeaveService(LeaveRequestRepository requests, LeaveBalanceRepository balances,
                        EmployeeRepository employees, LeaveDayCalculationService calculator,
                        CompanyTimeProvider time, AuditService audit, NotificationService notifications) {
        this.requests = requests;
        this.balances = balances;
        this.employees = employees;
        this.calculator = calculator;
        this.time = time;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional
    public LeaveRequestResponse apply(ApplyLeaveRequest input, AuthPrincipal principal) {
        Employee employee = employeeFor(principal);
        validateRange(input.startDate(), input.endDate(), true);
        if (requests.existsOverlap(employee.getId(), BLOCKING_STATUSES, input.startDate(), input.endDate())) {
            throw overlap();
        }
        List<LocalDate> leaveDates = calculator.calculate(employee.getId(), input.startDate(), input.endDate());
        if (input.leaveType().isPaid()) reserve(employee.getId(), input.leaveType(), leaveDates);

        LeaveRequest request = new LeaveRequest();
        request.setId(new ObjectId().toHexString());
        request.setEmployeeId(employee.getId());
        request.setEmployeeCodeSnapshot(employee.getEmployeeCode());
        request.setLeaveType(input.leaveType());
        request.setStartDate(input.startDate());
        request.setEndDate(input.endDate());
        request.setTotalDays(days(leaveDates.size()));
        request.setReason(input.reason().strip());
        request.setStatus(LeaveStatus.PENDING);
        request.setPaid(input.leaveType().isPaid());
        request.setLeaveDates(leaveDates);
        request.setCoveredDates(input.startDate().datesUntil(input.endDate().plusDays(1)).toList());
        request.setConflictActive(true);
        try {
            requests.save(request);
        } catch (DuplicateKeyException exception) {
            throw overlap();
        }
        audit.record(principal, "LEAVE_APPLIED", "LEAVE_REQUEST", request.getId(), Map.of(),
                Map.of("employeeId", employee.getId(), "leaveType", request.getLeaveType().name(),
                        "totalDays", request.getTotalDays(), "status", request.getStatus().name()), request.getReason());
        return LeaveRequestResponse.from(request);
    }

    public LeaveRequestPageResponse mine(AuthPrincipal principal, LeaveStatus status, LeaveType leaveType,
                                         LocalDate fromDate, LocalDate toDate, int page, int limit) {
        Employee employee = employeeFor(principal);
        return search(new LeaveSearch(employee.getId(), null, leaveType, status, fromDate, toDate,
                page, limit, "createdAt", "desc"));
    }

    public List<LeaveBalanceResponse> myBalances(AuthPrincipal principal, Integer year) {
        return balanceList(employeeFor(principal).getId(), year == null ? time.companyDate().getYear() : year);
    }

    @Transactional
    public LeaveRequestResponse cancel(String leaveId, AuthPrincipal principal) {
        Employee employee = employeeFor(principal);
        LeaveRequest request = find(leaveId);
        if (!request.getEmployeeId().equals(employee.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "The leave request does not belong to this employee");
        }
        requirePending(request);
        if (request.isPaid()) release(request);
        request.setStatus(LeaveStatus.CANCELLED);
        request.setConflictActive(false);
        requests.save(request);
        audit.record(principal, "LEAVE_CANCELLED", "LEAVE_REQUEST", request.getId(),
                Map.of("status", "PENDING"), Map.of("employeeId", employee.getId(), "status", "CANCELLED"), request.getReason());
        return LeaveRequestResponse.from(request);
    }

    public LeaveRequestPageResponse adminList(LeaveSearch search) { return search(search); }

    public LeaveRequestResponse detail(String leaveId) { return LeaveRequestResponse.from(find(leaveId)); }

    @Transactional
    public LeaveRequestResponse approve(String leaveId, AuthPrincipal admin) {
        LeaveRequest request = find(leaveId);
        requirePending(request);
        validateRange(request.getStartDate(), request.getEndDate(), false);
        employeeById(request.getEmployeeId());
        List<LocalDate> recalculated = calculator.calculate(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
        if (!recalculated.equals(request.getLeaveDates())) {
            throw new ConflictException("The employee schedule changed; reject this request and ask the employee to reapply");
        }
        if (requests.existsOverlap(request.getEmployeeId(), Set.of(LeaveStatus.APPROVED),
                request.getStartDate(), request.getEndDate())) throw overlap();
        if (request.isPaid()) approveReserved(request);
        request.setStatus(LeaveStatus.APPROVED);
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(time.now());
        requests.save(request);
        audit.record(admin, "LEAVE_APPROVED", "LEAVE_REQUEST", request.getId(),
                Map.of("status", "PENDING"), Map.of("employeeId", request.getEmployeeId(), "status", "APPROVED"), null);
        notifications.leaveApproved(request);
        return LeaveRequestResponse.from(request);
    }

    @Transactional
    public LeaveRequestResponse reject(String leaveId, RejectLeaveRequest input, AuthPrincipal admin) {
        LeaveRequest request = find(leaveId);
        requirePending(request);
        if (request.isPaid()) release(request);
        request.setStatus(LeaveStatus.REJECTED);
        request.setAdminRemarks(input.remarks().strip());
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(time.now());
        request.setConflictActive(false);
        requests.save(request);
        audit.record(admin, "LEAVE_REJECTED", "LEAVE_REQUEST", request.getId(),
                Map.of("status", "PENDING"), Map.of("employeeId", request.getEmployeeId(), "status", "REJECTED"), request.getAdminRemarks());
        notifications.leaveRejected(request);
        return LeaveRequestResponse.from(request);
    }

    public List<LeaveBalanceResponse> employeeBalances(String employeeId, Integer year) {
        employeeById(employeeId);
        return balanceList(employeeId, year == null ? time.companyDate().getYear() : year);
    }

    @Transactional
    public LeaveBalanceResponse allocate(String employeeId, AllocateLeaveBalanceRequest input, AuthPrincipal admin) {
        employeeById(employeeId);
        if (!input.leaveType().isPaid()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "LWP does not have an allocated balance");
        }
        BigDecimal allocation = normalize(input.allocatedDays());
        LeaveBalance balance = balances.findByEmployeeIdAndLeaveTypeAndYear(employeeId, input.leaveType(), input.year())
                .orElseGet(() -> newBalance(employeeId, input.leaveType(), input.year()));
        BigDecimal committed = balance.getUsedDays().add(balance.getPendingDays());
        if (allocation.compareTo(committed) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE, HttpStatus.CONFLICT,
                    "Allocation cannot be below used plus pending days");
        }
        BigDecimal before = balance.getAllocatedDays();
        balance.setAllocatedDays(allocation);
        balance.setAllocatedBy(admin.userId());
        recalculate(balance);
        balances.save(balance);
        audit.record(admin, "LEAVE_BALANCE_ALLOCATED", "LEAVE_BALANCE", balance.getId(),
                Map.of("allocatedDays", before), Map.of("employeeId", employeeId,
                        "leaveType", input.leaveType().name(), "year", input.year(), "allocatedDays", allocation), null);
        return LeaveBalanceResponse.from(balance);
    }

    public Set<LocalDate> approvedLeaveDays(String employeeId, LocalDate fromDate, LocalDate toDate) {
        return approvedLeaveTypes(employeeId, fromDate, toDate).keySet();
    }

    public Map<LocalDate, LeaveType> approvedLeaveTypes(String employeeId, LocalDate fromDate, LocalDate toDate) {
        validateRange(fromDate, toDate, false);
        Map<LocalDate, LeaveType> result = new TreeMap<>();
        for (LeaveRequest request : requests.findApprovedOverlapping(employeeId, fromDate, toDate)) {
            for (LocalDate date : request.getLeaveDates()) {
                if (!date.isBefore(fromDate) && !date.isAfter(toDate)) result.put(date, request.getLeaveType());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private LeaveRequestPageResponse search(LeaveSearch search) {
        validatePage(search.page(), search.limit());
        if (search.fromDate() != null && search.toDate() != null && search.toDate().isBefore(search.fromDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "toDate cannot be earlier than fromDate");
        }
        return LeaveRequestPageResponse.from(requests.search(search), search.limit());
    }

    private void reserve(String employeeId, LeaveType type, List<LocalDate> leaveDates) {
        for (Map.Entry<Integer, Long> entry : byYear(leaveDates).entrySet()) {
            LeaveBalance balance = balances.findByEmployeeIdAndLeaveTypeAndYear(employeeId, type, entry.getKey())
                    .orElseThrow(() -> insufficient(type, entry.getKey()));
            BigDecimal amount = days(entry.getValue());
            if (balance.getAvailableDays().compareTo(amount) < 0) throw insufficient(type, entry.getKey());
            balance.setPendingDays(balance.getPendingDays().add(amount));
            recalculate(balance);
            balances.save(balance);
        }
    }

    private void approveReserved(LeaveRequest request) {
        for (Map.Entry<Integer, Long> entry : byYear(request.getLeaveDates()).entrySet()) {
            LeaveBalance balance = requiredBalance(request, entry.getKey());
            BigDecimal amount = days(entry.getValue());
            if (balance.getPendingDays().compareTo(amount) < 0) throw inconsistentBalance();
            balance.setPendingDays(balance.getPendingDays().subtract(amount));
            balance.setUsedDays(balance.getUsedDays().add(amount));
            recalculate(balance);
            balances.save(balance);
        }
    }

    private void release(LeaveRequest request) {
        for (Map.Entry<Integer, Long> entry : byYear(request.getLeaveDates()).entrySet()) {
            LeaveBalance balance = requiredBalance(request, entry.getKey());
            BigDecimal amount = days(entry.getValue());
            if (balance.getPendingDays().compareTo(amount) < 0) throw inconsistentBalance();
            balance.setPendingDays(balance.getPendingDays().subtract(amount));
            recalculate(balance);
            balances.save(balance);
        }
    }

    private LeaveBalance requiredBalance(LeaveRequest request, int year) {
        return balances.findByEmployeeIdAndLeaveTypeAndYear(request.getEmployeeId(), request.getLeaveType(), year)
                .orElseThrow(this::inconsistentBalance);
    }

    private List<LeaveBalanceResponse> balanceList(String employeeId, int year) {
        if (year < 2000 || year > 2200) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Year is invalid");
        Map<LeaveType, LeaveBalance> stored = balances.findByEmployeeIdAndYearOrderByLeaveTypeAsc(employeeId, year).stream()
                .collect(Collectors.toMap(LeaveBalance::getLeaveType, Function.identity()));
        return Arrays.stream(LeaveType.values()).filter(LeaveType::isPaid)
                .map(type -> stored.getOrDefault(type, newBalance(employeeId, type, year)))
                .map(LeaveBalanceResponse::from).toList();
    }

    private static LeaveBalance newBalance(String employeeId, LeaveType type, int year) {
        LeaveBalance b = new LeaveBalance();
        b.setId(new ObjectId().toHexString());
        b.setEmployeeId(employeeId);
        b.setLeaveType(type);
        b.setYear(year);
        return b;
    }

    private static void recalculate(LeaveBalance balance) {
        BigDecimal available = balance.getAllocatedDays().subtract(balance.getUsedDays()).subtract(balance.getPendingDays());
        if (balance.getAllocatedDays().signum() < 0 || balance.getUsedDays().signum() < 0
                || balance.getPendingDays().signum() < 0 || available.signum() < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE, HttpStatus.CONFLICT, "Leave balance cannot be negative");
        }
        balance.setAvailableDays(available);
    }

    private Employee employeeFor(AuthPrincipal principal) {
        return employees.findByUserId(principal.userId()).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN,
                        "Employee account is inactive or unavailable"));
    }

    private Employee employeeById(String employeeId) {
        if (!ObjectId.isValid(employeeId)) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Employee id is invalid");
        return employees.findById(employeeId).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active employee was not found"));
    }

    private LeaveRequest find(String leaveId) {
        if (!ObjectId.isValid(leaveId)) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Leave request id is invalid");
        return requests.findById(leaveId).orElseThrow(() -> new ResourceNotFoundException("Leave request was not found"));
    }

    private void validateRange(LocalDate start, LocalDate end, boolean rejectPast) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Leave date range is invalid");
        }
        if (rejectPast && start.isBefore(time.companyDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Past-dated leave requests are not allowed");
        }
    }

    private static void requirePending(LeaveRequest request) {
        if (request.getStatus() != LeaveStatus.PENDING) throw new ConflictException("Only pending leave requests can be changed");
    }

    private static Map<Integer, Long> byYear(List<LocalDate> dates) {
        return dates.stream().collect(Collectors.groupingBy(LocalDate::getYear, TreeMap::new, Collectors.counting()));
    }

    private static BigDecimal days(long count) { return BigDecimal.valueOf(count).setScale(1); }
    private static BigDecimal normalize(BigDecimal value) { return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value; }
    private static void validatePage(int page, int limit) {
        if (page < 0 || limit < 1 || limit > 100) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "Page must be non-negative and limit must be between 1 and 100");
    }
    private static ApiException insufficient(LeaveType type, int year) {
        return new ApiException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE, HttpStatus.CONFLICT,
                "Insufficient " + type.name() + " balance for " + year);
    }
    private ApiException inconsistentBalance() {
        return new ApiException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "The reserved leave balance is inconsistent");
    }
    private static ApiException overlap() {
        return new ApiException(ErrorCode.OVERLAPPING_LEAVE, HttpStatus.CONFLICT,
                "A pending or approved leave request overlaps this date range");
    }
}
