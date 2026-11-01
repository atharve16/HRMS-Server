package com.hrms.attendance;

import com.hrms.attendance.model.AttendanceRecord;
import com.hrms.attendance.repository.AttendanceRepository;
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
import com.hrms.audit.service.AuditService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties={"app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters","app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters","app.auth.mail.from=noreply@example.com","spring.data.mongodb.auto-index-creation=true","management.health.mongo.enabled=false"})
class AttendanceMongoIntegrationTest {
 @Container static final MongoDBContainer MONGO=new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));
 @DynamicPropertySource static void mongo(DynamicPropertyRegistry r){r.add("spring.data.mongodb.uri",MONGO::getReplicaSetUrl);r.add("spring.data.mongodb.database",()->"hrms_attendance_test");}
 @Autowired AttendanceRepository records;@Autowired MongoTemplate template;@MockBean AuditService audit;
 @BeforeEach void clean(){records.deleteAll();doNothing().when(audit).record(any(),any(),any(),any(),any(),any(),any());}
 @Test void createsUniqueDailyAttendanceIndex(){var names=template.indexOps(AttendanceRecord.class).getIndexInfo().stream().map(i->i.getName()).toList();assertThat(names).contains("employee_work_date_unique","work_date_status","employee_work_date_query");}
 @Test void duplicateEmployeeAndWorkDateIsRejected(){AttendanceRecord one=new AttendanceRecord();one.setId("507f1f77bcf86cd799439011");one.setEmployeeId("e");one.setWorkDate(java.time.LocalDate.of(2026,11,1));records.save(one);AttendanceRecord two=new AttendanceRecord();two.setId("507f1f77bcf86cd799439012");two.setEmployeeId("e");two.setWorkDate(java.time.LocalDate.of(2026,11,1));assertThatThrownBy(()->records.save(two)).isInstanceOf(DuplicateKeyException.class);}
}
