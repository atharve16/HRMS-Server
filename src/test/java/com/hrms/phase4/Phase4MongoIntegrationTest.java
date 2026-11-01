package com.hrms.phase4;

import com.hrms.assignment.dto.CreateAssignmentRequest;
import com.hrms.assignment.model.EmployeeWorkAssignment;
import com.hrms.assignment.repository.AssignmentRepository;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.office.model.OfficeLocation;
import com.hrms.office.model.OfficeStatus;
import com.hrms.office.repository.OfficeLocationRepository;
import com.hrms.shift.model.Shift;
import com.hrms.shift.model.ShiftStatus;
import com.hrms.shift.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters", "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters", "app.auth.mail.from=noreply@example.com", "spring.data.mongodb.auto-index-creation=true", "management.health.mongo.enabled=false"})
class Phase4MongoIntegrationTest {
    @Container static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));
    @DynamicPropertySource static void mongodb(DynamicPropertyRegistry r) { r.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl); r.add("spring.data.mongodb.database", () -> "hrms_phase4_test"); }
    @Autowired EmployeeRepository employees; @Autowired ShiftRepository shifts; @Autowired OfficeLocationRepository offices; @Autowired AssignmentRepository assignments; @Autowired AssignmentService assignmentService; @Autowired MongoTemplate template;
    @MockBean AuditService audit;
    private final AuthPrincipal admin = new AuthPrincipal("admin", "admin@example.com", UserRole.ADMIN);

    @BeforeEach void clean(){assignments.deleteAll();employees.deleteAll();shifts.deleteAll();offices.deleteAll();doNothing().when(audit).record(any(),any(),any(),any(),any(),any(),any());}

    @Test void createsIndexesAndRejectsOverlappingActivePeriods(){
        Employee e=new Employee();e.setId("507f1f77bcf86cd799439011");e.setEmployeeCode("EMP-9001");e.setUserId("507f1f77bcf86cd799439012");e.setEmail("a@example.com");e.setFirstName("A");e.setLastName("A");e.setEmploymentStatus(EmploymentStatus.ACTIVE);employees.save(e);
        Shift s=new Shift();s.setId("507f1f77bcf86cd799439013");s.setShiftCode("DAY");s.setName("Day");s.setStartTime(LocalTime.of(9,0));s.setEndTime(LocalTime.of(17,0));s.setWorkingDays(EnumSet.of(DayOfWeek.MONDAY));s.setStatus(ShiftStatus.ACTIVE);shifts.save(s);
        OfficeLocation o=new OfficeLocation();o.setId("507f1f77bcf86cd799439014");o.setLocationCode("HQ");o.setName("HQ");o.setCoordinates(new GeoJsonPoint(79.98,23.18));o.setStatus(OfficeStatus.ACTIVE);offices.save(o);
        assignmentService.create(e.getId(),new CreateAssignmentRequest(s.getId(),o.getId(),LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),null),admin);
        assertThatThrownBy(() -> assignmentService.create(e.getId(),new CreateAssignmentRequest(s.getId(),o.getId(),LocalDate.of(2026,1,31),null,null),admin)).isInstanceOf(RuntimeException.class);
        assertThat(assignments.findByEmployeeIdOrderByEffectiveFromDesc(e.getId())).hasSize(1);
        assertThat(template.indexOps(OfficeLocation.class).getIndexInfo().stream().map(i->i.getName()).toList()).anyMatch(n->n.contains("2dsphere")||n.contains("coordinates"));
    }
}
