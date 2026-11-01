package com.hrms.payroll;
import com.hrms.audit.service.AuditService;import com.hrms.auth.entity.UserRole;import com.hrms.auth.security.AuthPrincipal;
import com.hrms.employee.model.Employee;import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.UpsertSalaryRequest;import com.hrms.payroll.model.SalaryStructure;import com.hrms.payroll.repository.SalaryStructureRepository;
import com.hrms.payroll.service.SalaryStructureService;import org.bson.types.ObjectId;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.LocalDate;import java.util.*;
import static org.assertj.core.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;

class SalaryStructureServiceTest {
 @Test void closesPriorEffectivePeriodAndKeepsHistory(){SalaryStructureRepository repo=mock(SalaryStructureRepository.class);EmployeeRepository employees=mock(EmployeeRepository.class);
  AuditService audit=mock(AuditService.class);SalaryStructureService service=new SalaryStructureService(repo,employees,audit);
  String employeeId=new ObjectId().toHexString();Employee employee=new Employee();employee.setId(employeeId);employee.setJoiningDate(LocalDate.of(2026,1,1));when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
  SalaryStructure old=new SalaryStructure();old.setId(new ObjectId().toHexString());old.setEmployeeId(employeeId);old.setEffectiveFrom(LocalDate.of(2026,1,1));old.setMonthlyFixedSalary(new BigDecimal("50000"));old.setActive(true);
  when(repo.findByEmployeeIdAndActiveTrue(employeeId)).thenReturn(Optional.of(old));when(repo.save(any())).thenAnswer(i->i.getArgument(0));
  service.upsert(employeeId,new UpsertSalaryRequest(new BigDecimal("60000"),LocalDate.of(2026,7,1)),new AuthPrincipal("admin","a@x.com",UserRole.ADMIN));
  assertThat(old.isActive()).isFalse();assertThat(old.getEffectiveTo()).isEqualTo(LocalDate.of(2026,6,30));
  verify(repo,times(2)).save(any(SalaryStructure.class));
 }
 @Test void rejectsSalaryEffectiveBeforeJoining(){SalaryStructureRepository repo=mock(SalaryStructureRepository.class);EmployeeRepository employees=mock(EmployeeRepository.class);
  String employeeId=new ObjectId().toHexString();Employee employee=new Employee();employee.setJoiningDate(LocalDate.of(2026,2,1));when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
  SalaryStructureService service=new SalaryStructureService(repo,employees,mock(AuditService.class));
  assertThatThrownBy(()->service.upsert(employeeId,new UpsertSalaryRequest(new BigDecimal("100"),LocalDate.of(2026,1,1)),new AuthPrincipal("admin","a@x.com",UserRole.ADMIN)))
    .hasMessageContaining("earlier than joining");verifyNoInteractions(repo);
 }
}
