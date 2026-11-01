package com.hrms.payroll;
import com.hrms.payroll.model.*;import com.hrms.payroll.repository.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;import org.springframework.data.mongodb.core.MongoTemplate;import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;import org.testcontainers.containers.MongoDBContainer;import org.testcontainers.junit.jupiter.*;import org.testcontainers.utility.DockerImageName;
import java.math.BigDecimal;import java.time.LocalDate;import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters","app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
 "app.auth.mail.from=noreply@example.com","spring.data.mongodb.auto-index-creation=true","management.health.mongo.enabled=false"})
class Phase8MongoIntegrationTest {
 @Container static final MongoDBContainer MONGO=new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));
 @DynamicPropertySource static void mongo(DynamicPropertyRegistry r){r.add("spring.data.mongodb.uri",MONGO::getReplicaSetUrl);r.add("spring.data.mongodb.database",()->"hrms_phase8_test");}
 @Autowired SalaryStructureRepository salaries;@Autowired PayrollRecordRepository payrolls;@Autowired MongoTemplate mongo;@Autowired TransactionTemplate transactions;
 @BeforeEach void clean(){payrolls.deleteAll();salaries.deleteAll();}
 @Test void createsIndexesAndPreventsPayrollAndOpenSalaryDuplicates(){
  assertThat(mongo.indexOps(SalaryStructure.class).getIndexInfo().stream().map(i->i.getName()).toList()).contains("employee_effective_from_unique","employee_active_salary_unique");
  assertThat(mongo.indexOps(PayrollRecord.class).getIndexInfo().stream().map(i->i.getName()).toList()).contains("employee_payroll_month_unique","payroll_month_year_status_idx","employee_payroll_year_idx");
  salaries.save(salary("507f1f77bcf86cd799439001","emp",LocalDate.of(2030,1,1),true));
  assertThatThrownBy(()->salaries.save(salary("507f1f77bcf86cd799439002","emp",LocalDate.of(2030,2,1),true))).isInstanceOf(DuplicateKeyException.class);
  payrolls.save(payroll("507f1f77bcf86cd799439010","emp",1,2030));
  assertThatThrownBy(()->payrolls.save(payroll("507f1f77bcf86cd799439011","emp",1,2030))).isInstanceOf(DuplicateKeyException.class);
 }
 @Test void transactionRollbackRemovesSalaryWrite(){transactions.executeWithoutResult(status->{salaries.save(salary("507f1f77bcf86cd799439001","emp",LocalDate.of(2030,1,1),true));status.setRollbackOnly();});assertThat(salaries.findAll()).isEmpty();}
 @Test void filtersAndPaginatesPayrollInMongo(){PayrollRecord first=payroll("507f1f77bcf86cd799439010","emp-a",1,2030);first.setEmployeeCodeSnapshot("EMP-1");first.setDepartmentIdSnapshot("dept-a");
  PayrollRecord second=payroll("507f1f77bcf86cd799439011","emp-b",2,2030);second.setEmployeeCodeSnapshot("EMP-2");second.setDepartmentIdSnapshot("dept-b");second.setStatus(PayrollStatus.FINALIZED);
  payrolls.saveAll(java.util.List.of(first,second));var page=payrolls.search(new PayrollSearch(2,2030,null,"EMP-2","dept-b",PayrollStatus.FINALIZED,0,10,"desc"));
  assertThat(page.getTotalElements()).isEqualTo(1);assertThat(page.getContent()).extracting(PayrollRecord::getEmployeeId).containsExactly("emp-b");}
 private static SalaryStructure salary(String id,String employee,LocalDate from,boolean active){SalaryStructure s=new SalaryStructure();s.setId(id);s.setEmployeeId(employee);s.setEffectiveFrom(from);s.setMonthlyFixedSalary(new BigDecimal("50000"));s.setActive(active);return s;}
 private static PayrollRecord payroll(String id,String employee,int month,int year){PayrollRecord p=new PayrollRecord();p.setId(id);p.setEmployeeId(employee);p.setPayrollMonth(month);p.setPayrollYear(year);p.setStatus(PayrollStatus.DRAFT);return p;}
}
