package com.hrms.payroll.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.employee.model.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.model.SalaryStructure;
import com.hrms.payroll.repository.SalaryStructureRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class SalaryStructureService {
    private final SalaryStructureRepository salaries; private final EmployeeRepository employees;
    private final AuditService audit;
    public SalaryStructureService(SalaryStructureRepository salaries,EmployeeRepository employees,AuditService audit){
        this.salaries=salaries;this.employees=employees;this.audit=audit;}

    @Transactional
    public SalaryStructureResponse upsert(String employeeId,UpsertSalaryRequest input,AuthPrincipal actor){
        Employee employee=employee(employeeId);
        if(input.effectiveFrom().isBefore(employee.getJoiningDate())) throw validation("Salary effective date cannot be earlier than joining date");
        BigDecimal amount=money(input.monthlyFixedSalary());
        SalaryStructure current=salaries.findByEmployeeIdAndActiveTrue(employeeId).orElse(null);
        if(current!=null&&input.effectiveFrom().isBefore(current.getEffectiveFrom())) throw validation("Salary changes cannot predate the current salary period");
        if(current!=null&&input.effectiveFrom().equals(current.getEffectiveFrom())){
            BigDecimal before=current.getMonthlyFixedSalary(); current.setMonthlyFixedSalary(amount); current.setUpdatedBy(actor.userId()); salaries.save(current);
            audit.record(actor,"SALARY_STRUCTURE_UPDATED","SALARY_STRUCTURE",current.getId(),Map.of("monthlyFixedSalary",before),
                    Map.of("employeeId",employeeId,"monthlyFixedSalary",amount),null);
            return SalaryStructureResponse.from(current);
        }
        if(current!=null){current.setEffectiveTo(input.effectiveFrom().minusDays(1));current.setActive(false);current.setUpdatedBy(actor.userId());salaries.save(current);}
        SalaryStructure salary=new SalaryStructure(); salary.setId(new ObjectId().toHexString()); salary.setEmployeeId(employeeId);
        salary.setMonthlyFixedSalary(amount);salary.setEffectiveFrom(input.effectiveFrom());salary.setActive(true);
        salary.setCreatedBy(actor.userId());salary.setUpdatedBy(actor.userId());salaries.save(salary);
        audit.record(actor,current==null?"SALARY_STRUCTURE_CREATED":"SALARY_STRUCTURE_UPDATED","SALARY_STRUCTURE",salary.getId(),Map.of(),
                Map.of("employeeId",employeeId,"monthlyFixedSalary",amount,"effectiveFrom",input.effectiveFrom().toString()),null);
        return SalaryStructureResponse.from(salary);
    }
    public SalaryStructureResponse current(String employeeId){employee(employeeId);SalaryStructure salary=salaries.findByEmployeeIdAndActiveTrue(employeeId)
            .orElseThrow(()->new ResourceNotFoundException("Salary structure was not found"));return SalaryStructureResponse.from(salary);}
    public List<SalaryStructureResponse> history(String employeeId){employee(employeeId);return salaries.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream().map(SalaryStructureResponse::from).toList();}
    public Optional<SalaryStructure> applicable(String employeeId,LocalDate date){List<SalaryStructure> matches=salaries.findApplicable(employeeId,date);
        if(matches.size()>1)throw new ConflictException("Overlapping salary periods were found for this employee");return matches.stream().findFirst();}
    private Employee employee(String id){if(!ObjectId.isValid(id))throw validation("Employee id is invalid");return employees.findById(id).orElseThrow(()->new ResourceNotFoundException("Employee was not found"));}
    private static BigDecimal money(BigDecimal v){if(v==null||v.signum()<=0)throw validation("Monthly fixed salary must be greater than zero");return v.setScale(2,RoundingMode.HALF_UP);}
    private static ApiException validation(String message){return new ApiException(ErrorCode.VALIDATION_ERROR,HttpStatus.BAD_REQUEST,message);}
}
