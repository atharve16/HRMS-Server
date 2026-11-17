package com.hrms.leave.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("leave_policies")
@CompoundIndex(name = "leave_policy_scope_type_employee_unique", def = "{'scope':1,'employeeId':1,'leaveType':1}", unique = true)
public class LeavePolicy {
    @Id private String id;
    private LeavePolicyScope scope;
    private String employeeId;
    private LeaveType leaveType;
    private BigDecimal monthlyEntitlement = BigDecimal.ZERO;
    private BigDecimal maxCarryForwardDays;
    private String updatedBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public LeavePolicyScope getScope() { return scope; } public void setScope(LeavePolicyScope v) { scope = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public LeaveType getLeaveType() { return leaveType; } public void setLeaveType(LeaveType v) { leaveType = v; }
    public BigDecimal getMonthlyEntitlement() { return monthlyEntitlement; } public void setMonthlyEntitlement(BigDecimal v) { monthlyEntitlement = v; }
    public BigDecimal getMaxCarryForwardDays() { return maxCarryForwardDays; } public void setMaxCarryForwardDays(BigDecimal v) { maxCarryForwardDays = v; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String v) { updatedBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
