package com.hrms.auth.security;

import com.hrms.HrmsApplication;
import com.hrms.auth.entity.AuthenticationIdentity;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.UserRepository;
import com.hrms.employee.service.EmployeeService;
import com.hrms.leave.service.LeaveService;
import com.hrms.holiday.service.HolidayService;
import com.hrms.notification.service.NotificationService;
import com.hrms.payroll.service.PayrollService;
import com.hrms.payroll.service.SalaryStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {HrmsApplication.class, AuthenticationSecurityIntegrationTest.SecurityProbeController.class},
        properties = {
                "spring.data.mongodb.auto-index-creation=false",
                "management.health.mongo.enabled=false",
                "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
                "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
                "app.auth.mail.from=noreply@example.com"
        })
@AutoConfigureMockMvc
class AuthenticationSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private UserRepository userRepository;
    @MockBean private EmployeeService employeeService;
    @MockBean private LeaveService leaveService;
    @MockBean private HolidayService holidayService;
    @MockBean private NotificationService notificationService;
    @MockBean private PayrollService payrollService;
    @MockBean private SalaryStructureService salaryStructureService;
    private User admin;
    private User employee;

    @BeforeEach
    void setUp() {
        admin = user("admin-id", "admin@example.com", UserRole.ADMIN);
        employee = user("employee-id", "employee@example.com", UserRole.EMPLOYEE);
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(userRepository.findById("employee-id")).thenReturn(Optional.of(employee));
    }

    @Test
    void authMeRejectsAnonymousAndReturnsSafeAuthenticatedProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("employee-id"))
                .andExpect(jsonPath("$.data.email").value("employee@example.com"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.employeeId").doesNotExist());
    }

    @Test
    void enforcesAdminEmployeeAndSharedAuthorizationPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-probe").header("Authorization", bearer(employee)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/security-probe").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employee/security-probe").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/employee/security-probe").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/attendance/security-probe").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/attendance/security-probe").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void protectsEmployeeAdministrationAndAllowsSelfProfileOnlyToEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/admin/employees").header("Authorization", bearer(employee)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/employees").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/employees/me").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/employees/me").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void enforcesEmployeeAndAdminLeaveOperations() throws Exception {
        String application = "{\"leaveType\":\"CL\",\"startDate\":\"2030-01-07\",\"endDate\":\"2030-01-07\",\"reason\":\"Personal\"}";
        mockMvc.perform(post("/api/v1/leaves").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(application))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/leaves").header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content(application))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/leaves/507f1f77bcf86cd799439011/approve")
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/leaves/507f1f77bcf86cd799439011/approve")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        String allocation = "{\"leaveType\":\"CL\",\"year\":2030,\"allocatedDays\":12}";
        mockMvc.perform(put("/api/v1/admin/employees/507f1f77bcf86cd799439011/leave-balance")
                        .header("Authorization", bearer(employee)).contentType(MediaType.APPLICATION_JSON).content(allocation))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/employees/507f1f77bcf86cd799439011/leave-balance")
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(allocation))
                .andExpect(status().isOk());
    }

    @Test
    void enforcesHolidayAdministrationAndEmployeeNotificationAccess() throws Exception {
        String holiday = "{\"name\":\"Republic Day\",\"date\":\"2030-01-26\"}";
        mockMvc.perform(post("/api/v1/admin/holidays").header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content(holiday))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/holidays").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(holiday))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/holidays").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/holidays").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void enforcesAdminPayrollOperationsAndEmployeePayrollVisibility() throws Exception {
        String generation = "{\"month\":1,\"year\":2030}";
        mockMvc.perform(post("/api/v1/admin/payroll/generate").header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content(generation))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/payroll/generate").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(generation))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/payroll/me").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/payroll/me").header("Authorization", bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInactiveUsersAndUnknownRoutesByDefault() throws Exception {
        employee.setStatus(UserStatus.INACTIVE);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(employee)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/unconfigured").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("FORBIDDEN"));
    }

    @Test
    void openApiIncludesAuthenticationContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/request-otp']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/verify-otp']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/leaves']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/leaves/me/balance']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/leaves/{leaveId}/approve']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/employees/{employeeId}/leave-balance']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/holidays']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/holidays/upcoming']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/notifications/unread-count']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/notifications/{notificationId}/read']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/employees/{employeeId}/salary']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/payroll/generate']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/payroll/{payrollId}/finalize']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payroll/me']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    void otpRequestIsNonEnumeratingAndRefreshRequiresCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unregistered@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unregistered@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("INVALID_OTP"));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("INVALID_REFRESH_TOKEN"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.issueAccessToken(user);
    }

    private User user(String id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setNormalizedEmail(email);
        user.setAuthenticationIdentity(AuthenticationIdentity.PASSWORDLESS_EMAIL_OTP);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @RestController
    static class SecurityProbeController {
        @GetMapping("/api/v1/admin/security-probe") String admin() { return "admin"; }
        @GetMapping("/api/v1/employee/security-probe") String employee() { return "employee"; }
        @GetMapping("/api/v1/attendance/security-probe") String shared() { return "shared"; }
    }
}
