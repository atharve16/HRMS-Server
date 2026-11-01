package com.hrms.employee;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSearch;
import com.hrms.employee.service.EmployeeCodeGenerator;
import com.hrms.employee.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceTest {
    private static final String EMPLOYEE_ID = "507f1f77bcf86cd799439011";
    private static final String USER_ID = "507f1f77bcf86cd799439012";
    @Mock private EmployeeRepository employees;
    @Mock private UserRepository users;
    @Mock private EmployeeCodeGenerator codes;
    @Mock private AuditService audit;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmployeeService(employees, users, codes, audit,
                new com.hrms.auth.service.EmailNormalizer(),
                Clock.fixed(Instant.parse("2026-11-01T05:30:00Z"), ZoneOffset.UTC));
        when(employees.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(audit).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsLinkedEmployeeAndUserWithImmutableGeneratedCode() {
        when(users.findByNormalizedEmail("employee@example.com")).thenReturn(Optional.empty());
        when(employees.findByEmail("employee@example.com")).thenReturn(Optional.empty());
        when(codes.next()).thenReturn("EMP-0001");

        var response = service.create(EmployeeTestFixtures.create(" Employee@Example.com "), EmployeeTestFixtures.admin());

        assertThat(response.employeeCode()).isEqualTo("EMP-0001");
        assertThat(response.email()).isEqualTo("employee@example.com");
        verify(employees).save(any(Employee.class));
        verify(users).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getRole().name().equals("EMPLOYEE")
                        && user.getEmployeeId().equals(response.id())
                        && user.getId().equals(response.userId())));
        verify(audit).record(any(), org.mockito.ArgumentMatchers.eq("EMPLOYEE_CREATED"),
                org.mockito.ArgumentMatchers.eq("EMPLOYEE"), org.mockito.ArgumentMatchers.eq(response.id()),
                any(), any(), any());
    }

    @Test
    void rejectsDuplicateEmailBeforeCreatingEitherDocument() {
        User existing = User.initialAdmin("employee@example.com");
        existing.setId("existing");
        when(users.findByNormalizedEmail("employee@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(EmployeeTestFixtures.create("employee@example.com"), EmployeeTestFixtures.admin()))
                .isInstanceOf(com.hrms.common.exceptions.ConflictException.class);
        verify(employees, never()).save(any());
        verify(users, never()).save(any());
    }

    @Test
    void updatesEmailOnBothDocuments() {
        Employee employee = employee();
        User user = User.employee(USER_ID, "old@example.com", EMPLOYEE_ID);
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(users.findByNormalizedEmail("new@example.com")).thenReturn(Optional.empty());
        when(employees.findByEmail("new@example.com")).thenReturn(Optional.empty());

        service.update(EMPLOYEE_ID, EmployeeTestFixtures.updateEmail("new@example.com"), EmployeeTestFixtures.admin());

        assertThat(employee.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getNormalizedEmail()).isEqualTo("new@example.com");
        verify(users).save(user);
        verify(audit).record(any(), org.mockito.ArgumentMatchers.eq("EMPLOYEE_UPDATED"),
                any(), any(), any(), any(), any());
    }

    @Test
    void deactivationAndReactivationSynchronizeAuthenticationStatus() {
        Employee employee = employee();
        User user = User.employee(USER_ID, employee.getEmail(), EMPLOYEE_ID);
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        service.deactivate(EMPLOYEE_ID, EmployeeTestFixtures.admin());
        assertThat(employee.getEmploymentStatus()).isEqualTo(EmploymentStatus.INACTIVE);
        assertThat(user.getStatus().name()).isEqualTo("INACTIVE");

        service.reactivate(EMPLOYEE_ID, EmployeeTestFixtures.admin());
        assertThat(employee.getEmploymentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void selfProfileUsesAuthenticatedUserLinkAndRejectsInvalidIds() {
        Employee employee = employee();
        when(employees.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        assertThat(service.current(new com.hrms.auth.security.AuthPrincipal(USER_ID, "employee@example.com",
                com.hrms.auth.entity.UserRole.EMPLOYEE))).isNotNull();

        assertThatThrownBy(() -> service.get("not-an-object-id"))
                .isInstanceOf(com.hrms.common.exceptions.ApiException.class);
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        employee.setUserId(USER_ID);
        employee.setEmployeeCode("EMP-0001");
        employee.setEmail("old@example.com");
        employee.setFirstName("Ada");
        employee.setLastName("Lovelace");
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        return employee;
    }
}
