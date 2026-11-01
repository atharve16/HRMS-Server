package com.hrms.leave;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.model.*;
import com.hrms.leave.repository.*;
import com.hrms.leave.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
        "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
        "app.auth.mail.from=noreply@example.com", "spring.data.mongodb.auto-index-creation=true",
        "management.health.mongo.enabled=false"})
class LeaveMongoIntegrationTest {
    @Container static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));
    @DynamicPropertySource static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "hrms_leave_test");
    }

    @Autowired LeaveRequestRepository requests;
    @Autowired LeaveBalanceRepository balances;
    @Autowired EmployeeRepository employees;
    @Autowired LeaveService service;
    @Autowired MongoTemplate template;
    @MockBean LeaveDayCalculationService calculator;
    @MockBean AuditService audit;
    private final AuthPrincipal employee = new AuthPrincipal("507f1f77bcf86cd799439002", "e@example.com", UserRole.EMPLOYEE);

    @BeforeEach
    void clean() {
        requests.deleteAll(); balances.deleteAll(); employees.deleteAll();
        reset(calculator, audit);
        doNothing().when(audit).record(any(), any(), any(), any(), any(), any(), any());
        Employee e = new Employee(); e.setId("507f1f77bcf86cd799439001"); e.setUserId(employee.userId());
        e.setEmployeeCode("EMP-1001"); e.setEmail("e@example.com"); e.setFirstName("E"); e.setLastName("One");
        e.setEmploymentStatus(EmploymentStatus.ACTIVE); employees.save(e);
    }

    @Test
    void createsRequiredIndexesAndPreventsConcurrentStyleOverlap() {
        List<String> balanceIndexes = template.indexOps(LeaveBalance.class).getIndexInfo().stream().map(i -> i.getName()).toList();
        List<String> requestIndexes = template.indexOps(LeaveRequest.class).getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(balanceIndexes).contains("employee_leave_type_year_unique");
        assertThat(requestIndexes).contains("employee_start_date_idx", "employee_status_idx", "status_start_date_idx",
                "leave_type_status_idx", "active_employee_covered_date_unique");

        LocalDate date = LocalDate.of(2030, 1, 7);
        requests.save(request("507f1f77bcf86cd799439010", "EMP-1001", LeaveType.CL, LeaveStatus.PENDING, date));
        LeaveRequest overlapping = request("507f1f77bcf86cd799439011", "EMP-1001", LeaveType.SL, LeaveStatus.PENDING, date);
        assertThatThrownBy(() -> requests.save(overlapping)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void transactionRollsBackReservedBalanceWhenRequestInsertConflicts() {
        LocalDate requestedDate = LocalDate.of(2030, 1, 7);
        LeaveRequest blocker = request("507f1f77bcf86cd799439010", "EMP-1001", LeaveType.SL,
                LeaveStatus.PENDING, LocalDate.of(2030, 2, 1));
        blocker.setCoveredDates(List.of(requestedDate));
        requests.save(blocker);

        LeaveBalance balance = new LeaveBalance(); balance.setId("507f1f77bcf86cd799439020");
        balance.setEmployeeId("507f1f77bcf86cd799439001"); balance.setLeaveType(LeaveType.CL); balance.setYear(2030);
        balance.setAllocatedDays(new BigDecimal("3")); balance.setUsedDays(BigDecimal.ZERO);
        balance.setPendingDays(BigDecimal.ZERO); balance.setAvailableDays(new BigDecimal("3")); balances.save(balance);
        when(calculator.calculate("507f1f77bcf86cd799439001", requestedDate, requestedDate)).thenReturn(List.of(requestedDate));

        assertThatThrownBy(() -> service.apply(new ApplyLeaveRequest(LeaveType.CL, requestedDate, requestedDate, "Reason"), employee))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("overlaps");
        LeaveBalance reloaded = balances.findById(balance.getId()).orElseThrow();
        assertThat(reloaded.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reloaded.getAvailableDays()).isEqualByComparingTo("3");
    }

    @Test
    void filtersAndPaginatesInMongoAndRetrievesApprovedDays() {
        requests.save(request("507f1f77bcf86cd799439010", "EMP-1001", LeaveType.CL,
                LeaveStatus.APPROVED, LocalDate.of(2030, 1, 7)));
        requests.save(request("507f1f77bcf86cd799439011", "EMP-2000", LeaveType.LWP,
                LeaveStatus.REJECTED, LocalDate.of(2030, 2, 7)));

        var page = requests.search(new LeaveSearch(null, "EMP-1001", LeaveType.CL, LeaveStatus.APPROVED,
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), 0, 1, "startDate", "asc"));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(LeaveRequest::getEmployeeCodeSnapshot).containsExactly("EMP-1001");
        assertThat(service.approvedLeaveDays("507f1f77bcf86cd799439001",
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))).containsExactly(LocalDate.of(2030, 1, 7));
    }

    private static LeaveRequest request(String id, String code, LeaveType type, LeaveStatus status, LocalDate date) {
        LeaveRequest r = new LeaveRequest(); r.setId(id); r.setEmployeeId("507f1f77bcf86cd799439001");
        r.setEmployeeCodeSnapshot(code); r.setLeaveType(type); r.setStartDate(date); r.setEndDate(date);
        r.setTotalDays(BigDecimal.ONE); r.setReason("Reason"); r.setStatus(status); r.setPaid(type.isPaid());
        r.setLeaveDates(List.of(date)); r.setCoveredDates(List.of(date));
        r.setConflictActive(status == LeaveStatus.PENDING || status == LeaveStatus.APPROVED); return r;
    }
}
