package com.hrms.payroll;
import com.hrms.audit.service.AuditService;import com.hrms.auth.entity.UserRole;import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ApiException;import com.hrms.common.utils.CompanyTimeProvider;import com.hrms.employee.model.*;import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.*;import com.hrms.payroll.model.*;import com.hrms.payroll.repository.*;import com.hrms.payroll.service.*;
import org.bson.types.ObjectId;import org.junit.jupiter.api.*;import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;import java.time.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;

class PayrollServiceTest {
 PayrollRecordRepository payrolls=mock(PayrollRecordRepository.class);EmployeeRepository employees=mock(EmployeeRepository.class);
 SalaryStructureService salaries=mock(SalaryStructureService.class);PayrollCalculationService calculator=mock(PayrollCalculationService.class);
 AuditService audit=mock(AuditService.class);CompanyTimeProvider time=mock(CompanyTimeProvider.class);TransactionTemplate transactions=mock(TransactionTemplate.class);
 PayrollService service=new PayrollService(payrolls,employees,salaries,calculator,audit,time,transactions);AuthPrincipal admin=new AuthPrincipal("admin","a@x.com",UserRole.ADMIN);
 PayrollRecord record;
 @BeforeEach void setup(){record=new PayrollRecord();record.setId(new ObjectId().toHexString());record.setEmployeeId(new ObjectId().toHexString());record.setStatus(PayrollStatus.DRAFT);
  record.setMonthlyFixedSalarySnapshot(new BigDecimal("1000.00"));record.setSalaryDeduction(new BigDecimal("100.00"));record.setEarnings(new ArrayList<>());record.setDeductions(new ArrayList<>());
  when(payrolls.findById(record.getId())).thenReturn(Optional.of(record));when(payrolls.save(any())).thenAnswer(i->i.getArgument(0));when(time.now()).thenReturn(Instant.parse("2026-11-01T00:00:00Z"));}

 @Test void draftAdjustmentsRecalculateAndLifecycleMakesPayrollImmutable(){UpdateDraftPayrollRequest update=new UpdateDraftPayrollRequest(
   List.of(new PayrollAdjustmentInput(PayrollAdjustmentType.BONUS,"Performance bonus",new BigDecimal("100")),new PayrollAdjustmentInput(PayrollAdjustmentType.OVERTIME_PAY,"Approved overtime",new BigDecimal("50"))),
   List.of(new PayrollAdjustmentInput(PayrollAdjustmentType.OTHER_DEDUCTION,"Recovery",new BigDecimal("25"))));
  PayrollResponse adjusted=service.updateDraft(record.getId(),update,admin);assertThat(adjusted.totalEarnings()).isEqualByComparingTo("150.00");
  assertThat(adjusted.totalDeductions()).isEqualByComparingTo("125.00");assertThat(adjusted.netSalary()).isEqualByComparingTo("1025.00");
  service.review(record.getId(),admin);service.finalizePayroll(record.getId(),admin);service.pay(record.getId(),admin);assertThat(record.getStatus()).isEqualTo(PayrollStatus.PAID);
  assertThatThrownBy(()->service.updateDraft(record.getId(),update,admin)).isInstanceOf(ApiException.class).hasMessageContaining("Only draft");
 }

 @Test void rejectsAdjustmentsThatWouldMakeNetNegative(){UpdateDraftPayrollRequest update=new UpdateDraftPayrollRequest(List.of(),
  List.of(new PayrollAdjustmentInput(PayrollAdjustmentType.OTHER_DEDUCTION,"Large recovery",new BigDecimal("901"))));
  assertThatThrownBy(()->service.updateDraft(record.getId(),update,admin)).isInstanceOf(ApiException.class).hasMessageContaining("negative net salary");}

 @Test void duplicatePayrollIsSkippedWithoutOverwrite(){Employee employee=new Employee();employee.setId(record.getEmployeeId());employee.setEmployeeCode("EMP-1");
  employee.setJoiningDate(LocalDate.of(2026,1,1));employee.setEmploymentStatus(EmploymentStatus.ACTIVE);when(employees.findAll()).thenReturn(List.of(employee));
  when(payrolls.existsByEmployeeIdAndPayrollMonthAndPayrollYear(employee.getId(),11,2026)).thenReturn(true);
  PayrollGenerationSummary summary=service.generate(new GeneratePayrollRequest(11,2026),admin);assertThat(summary.generated()).isEmpty();assertThat(summary.skipped()).hasSize(1);verifyNoInteractions(calculator);}

 @Test void employeeCannotReadAnotherEmployeesPayroll(){Employee employee=new Employee();employee.setId(new ObjectId().toHexString());employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
  AuthPrincipal principal=new AuthPrincipal("user","e@x.com",UserRole.EMPLOYEE);when(employees.findByUserId("user")).thenReturn(Optional.of(employee));
  when(payrolls.findByIdAndEmployeeIdAndStatusIn(eq(record.getId()),eq(employee.getId()),anyCollection())).thenReturn(Optional.empty());
  assertThatThrownBy(()->service.mineDetail(record.getId(),principal)).hasMessageContaining("Visible payroll record was not found");
  verify(payrolls).findByIdAndEmployeeIdAndStatusIn(eq(record.getId()),eq(employee.getId()),argThat(statuses->!statuses.contains(PayrollStatus.DRAFT)&&statuses.contains(PayrollStatus.FINALIZED)));
 }
}
