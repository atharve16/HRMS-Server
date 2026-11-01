package com.hrms.payroll.dto;
import com.hrms.payroll.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PayrollResponse(String id,String employeeId,String employeeCodeSnapshot,String employeeNameSnapshot,
 String departmentIdSnapshot,String departmentSnapshot,int payrollMonth,int payrollYear,BigDecimal monthlyFixedSalarySnapshot,
 int scheduledWorkingDays,int attendedDays,int paidLeaveDays,int unpaidLeaveDays,int unexcusedAbsenceDays,int totalUnpaidDays,
 BigDecimal dailySalaryRate,BigDecimal salaryDeduction,List<PayrollAdjustment> earnings,List<PayrollAdjustment> deductions,
 BigDecimal totalEarnings,BigDecimal totalDeductions,BigDecimal netSalary,PayrollStatus status,
 PayrollCalculationDetails calculationDetails,String reviewedBy,Instant reviewedAt,String finalizedBy,Instant finalizedAt,
 String paidBy,Instant paidAt,Instant createdAt,Instant updatedAt,long version) {
 public static PayrollResponse from(PayrollRecord p){return new PayrollResponse(p.getId(),p.getEmployeeId(),p.getEmployeeCodeSnapshot(),
 p.getEmployeeNameSnapshot(),p.getDepartmentIdSnapshot(),p.getDepartmentSnapshot(),p.getPayrollMonth(),p.getPayrollYear(),
 p.getMonthlyFixedSalarySnapshot(),p.getScheduledWorkingDays(),p.getAttendedDays(),p.getPaidLeaveDays(),p.getUnpaidLeaveDays(),
 p.getUnexcusedAbsenceDays(),p.getTotalUnpaidDays(),p.getDailySalaryRate(),p.getSalaryDeduction(),p.getEarnings(),p.getDeductions(),
 p.getTotalEarnings(),p.getTotalDeductions(),p.getNetSalary(),p.getStatus(),p.getCalculationDetails(),p.getReviewedBy(),p.getReviewedAt(),
 p.getFinalizedBy(),p.getFinalizedAt(),p.getPaidBy(),p.getPaidAt(),p.getCreatedAt(),p.getUpdatedAt(),p.getVersion());}
}
