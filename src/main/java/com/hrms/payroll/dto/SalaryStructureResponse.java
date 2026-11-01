package com.hrms.payroll.dto;
import com.hrms.payroll.model.SalaryStructure;
import java.math.BigDecimal;
import java.time.*;
public record SalaryStructureResponse(String id,String employeeId,BigDecimal monthlyFixedSalary,
        LocalDate effectiveFrom,LocalDate effectiveTo,boolean active,Instant createdAt,Instant updatedAt) {
    public static SalaryStructureResponse from(SalaryStructure s){return new SalaryStructureResponse(s.getId(),s.getEmployeeId(),
            s.getMonthlyFixedSalary(),s.getEffectiveFrom(),s.getEffectiveTo(),s.isActive(),s.getCreatedAt(),s.getUpdatedAt());}
}
