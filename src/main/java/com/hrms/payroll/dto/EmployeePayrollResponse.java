package com.hrms.payroll.dto;
import com.hrms.payroll.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record EmployeePayrollResponse(String id,String employeeCode,String employeeName,String department,int payrollMonth,int payrollYear,
 BigDecimal monthlyFixedSalary,int scheduledWorkingDays,int attendedDays,int paidLeaveDays,int unpaidLeaveDays,int unexcusedAbsenceDays,
 int totalUnpaidDays,BigDecimal dailySalaryRate,BigDecimal salaryDeduction,List<PayrollAdjustment> earnings,
 List<PayrollAdjustment> deductions,BigDecimal totalEarnings,BigDecimal totalDeductions,BigDecimal netSalary,PayrollStatus status,
 PayrollCalculationDetails calculationDetails,Instant finalizedAt,Instant paidAt){
 public static EmployeePayrollResponse from(PayrollRecord p){return new EmployeePayrollResponse(p.getId(),p.getEmployeeCodeSnapshot(),
 p.getEmployeeNameSnapshot(),p.getDepartmentSnapshot(),p.getPayrollMonth(),p.getPayrollYear(),p.getMonthlyFixedSalarySnapshot(),
 p.getScheduledWorkingDays(),p.getAttendedDays(),p.getPaidLeaveDays(),p.getUnpaidLeaveDays(),p.getUnexcusedAbsenceDays(),p.getTotalUnpaidDays(),
 p.getDailySalaryRate(),p.getSalaryDeduction(),p.getEarnings(),p.getDeductions(),p.getTotalEarnings(),p.getTotalDeductions(),
 p.getNetSalary(),p.getStatus(),p.getCalculationDetails(),p.getFinalizedAt(),p.getPaidAt());}
}
