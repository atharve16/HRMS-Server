package com.hrms.payroll.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Document("payroll_records")
@CompoundIndexes({
        @CompoundIndex(name="employee_payroll_month_unique",def="{'employeeId':1,'payrollMonth':1,'payrollYear':1}",unique=true),
        @CompoundIndex(name="payroll_month_year_status_idx",def="{'payrollMonth':1,'payrollYear':1,'status':1}"),
        @CompoundIndex(name="employee_payroll_year_idx",def="{'employeeId':1,'payrollYear':-1}")
})
public class PayrollRecord {
    @Id private String id;
    private String employeeId;
    private String employeeCodeSnapshot;
    private String employeeNameSnapshot;
    private String departmentIdSnapshot;
    private String departmentSnapshot;
    private int payrollMonth;
    private int payrollYear;
    private BigDecimal monthlyFixedSalarySnapshot;
    private int scheduledWorkingDays;
    private int attendedDays;
    private int paidLeaveDays;
    private int unpaidLeaveDays;
    private int unexcusedAbsenceDays;
    private int totalUnpaidDays;
    private BigDecimal dailySalaryRate;
    private BigDecimal salaryDeduction;
    private List<PayrollAdjustment> earnings=new ArrayList<>();
    private List<PayrollAdjustment> deductions=new ArrayList<>();
    private BigDecimal totalEarnings=BigDecimal.ZERO.setScale(2);
    private BigDecimal totalDeductions=BigDecimal.ZERO.setScale(2);
    private BigDecimal netSalary;
    private PayrollStatus status;
    private PayrollCalculationDetails calculationDetails;
    private String reviewedBy; private Instant reviewedAt;
    private String finalizedBy; private Instant finalizedAt;
    private String paidBy; private Instant paidAt;
    private String createdBy; private String updatedBy;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getEmployeeCodeSnapshot(){return employeeCodeSnapshot;} public void setEmployeeCodeSnapshot(String v){employeeCodeSnapshot=v;}
    public String getEmployeeNameSnapshot(){return employeeNameSnapshot;} public void setEmployeeNameSnapshot(String v){employeeNameSnapshot=v;}
    public String getDepartmentIdSnapshot(){return departmentIdSnapshot;} public void setDepartmentIdSnapshot(String v){departmentIdSnapshot=v;}
    public String getDepartmentSnapshot(){return departmentSnapshot;} public void setDepartmentSnapshot(String v){departmentSnapshot=v;}
    public int getPayrollMonth(){return payrollMonth;} public void setPayrollMonth(int v){payrollMonth=v;}
    public int getPayrollYear(){return payrollYear;} public void setPayrollYear(int v){payrollYear=v;}
    public BigDecimal getMonthlyFixedSalarySnapshot(){return monthlyFixedSalarySnapshot;} public void setMonthlyFixedSalarySnapshot(BigDecimal v){monthlyFixedSalarySnapshot=v;}
    public int getScheduledWorkingDays(){return scheduledWorkingDays;} public void setScheduledWorkingDays(int v){scheduledWorkingDays=v;}
    public int getAttendedDays(){return attendedDays;} public void setAttendedDays(int v){attendedDays=v;}
    public int getPaidLeaveDays(){return paidLeaveDays;} public void setPaidLeaveDays(int v){paidLeaveDays=v;}
    public int getUnpaidLeaveDays(){return unpaidLeaveDays;} public void setUnpaidLeaveDays(int v){unpaidLeaveDays=v;}
    public int getUnexcusedAbsenceDays(){return unexcusedAbsenceDays;} public void setUnexcusedAbsenceDays(int v){unexcusedAbsenceDays=v;}
    public int getTotalUnpaidDays(){return totalUnpaidDays;} public void setTotalUnpaidDays(int v){totalUnpaidDays=v;}
    public BigDecimal getDailySalaryRate(){return dailySalaryRate;} public void setDailySalaryRate(BigDecimal v){dailySalaryRate=v;}
    public BigDecimal getSalaryDeduction(){return salaryDeduction;} public void setSalaryDeduction(BigDecimal v){salaryDeduction=v;}
    public List<PayrollAdjustment> getEarnings(){return earnings;} public void setEarnings(List<PayrollAdjustment> v){earnings=v;}
    public List<PayrollAdjustment> getDeductions(){return deductions;} public void setDeductions(List<PayrollAdjustment> v){deductions=v;}
    public BigDecimal getTotalEarnings(){return totalEarnings;} public void setTotalEarnings(BigDecimal v){totalEarnings=v;}
    public BigDecimal getTotalDeductions(){return totalDeductions;} public void setTotalDeductions(BigDecimal v){totalDeductions=v;}
    public BigDecimal getNetSalary(){return netSalary;} public void setNetSalary(BigDecimal v){netSalary=v;}
    public PayrollStatus getStatus(){return status;} public void setStatus(PayrollStatus v){status=v;}
    public PayrollCalculationDetails getCalculationDetails(){return calculationDetails;} public void setCalculationDetails(PayrollCalculationDetails v){calculationDetails=v;}
    public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=v;} public Instant getReviewedAt(){return reviewedAt;} public void setReviewedAt(Instant v){reviewedAt=v;}
    public String getFinalizedBy(){return finalizedBy;} public void setFinalizedBy(String v){finalizedBy=v;} public Instant getFinalizedAt(){return finalizedAt;} public void setFinalizedAt(Instant v){finalizedAt=v;}
    public String getPaidBy(){return paidBy;} public void setPaidBy(String v){paidBy=v;} public Instant getPaidAt(){return paidAt;} public void setPaidAt(Instant v){paidAt=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;} public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
