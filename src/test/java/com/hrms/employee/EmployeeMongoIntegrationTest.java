package com.hrms.employee;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.employee.model.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSearch;
import com.hrms.employee.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
        "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
        "app.auth.mail.from=noreply@example.com",
        "spring.data.mongodb.auto-index-creation=true",
        "management.health.mongo.enabled=false"
})
class EmployeeMongoIntegrationTest {
    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));

    @DynamicPropertySource
    static void mongodbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "hrms_employee_test");
    }

    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeRepository employees;
    @Autowired private UserRepository users;
    @Autowired private MongoTemplate mongoTemplate;
    @MockBean private AuditService auditService;

    @BeforeEach
    void clean() {
        employees.deleteAll();
        users.deleteAll();
        mongoTemplate.dropCollection("employee_code_counters");
        doNothing().when(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsLinkedDocumentsAndSearchesWithDatabasePagination() {
        var first = employeeService.create(EmployeeTestFixtures.create("ada@example.com"), EmployeeTestFixtures.admin());
        var secondRequest = EmployeeTestFixtures.create("grace@example.com");
        var second = employeeService.create(secondRequest, EmployeeTestFixtures.admin());

        assertThat(first.employeeCode()).isEqualTo("EMP-0001");
        assertThat(second.employeeCode()).isEqualTo("EMP-0002");
        Employee stored = employees.findById(first.id()).orElseThrow();
        User linked = users.findById(first.userId()).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(linked.getId());
        assertThat(linked.getEmployeeId()).isEqualTo(stored.getId());

        var page = employeeService.list(new EmployeeSearch("Ada", null, null, null, null, 0, 1,
                "employeeCode", "asc"));
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).email()).isEqualTo("ada@example.com");
    }

    @Test
    void rollsBackEmployeeAndUserWhenLaterWorkFails() {
        doThrow(new IllegalStateException("audit failure"))
                .when(auditService).record(any(), any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> employeeService.create(
                EmployeeTestFixtures.create("rollback@example.com"), EmployeeTestFixtures.admin()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(employees.findByEmail("rollback@example.com")).isEmpty();
        assertThat(users.findByNormalizedEmail("rollback@example.com")).isEmpty();
    }

    @Test
    void createsRequiredUniqueAndFilterIndexes() {
        List<String> indexNames = mongoTemplate.indexOps(Employee.class).getIndexInfo().stream()
                .map(index -> index.getName()).toList();
        assertThat(indexNames).contains("employeeCode", "userId", "email", "departmentId",
                "employmentStatus", "joiningDate");
    }
}
