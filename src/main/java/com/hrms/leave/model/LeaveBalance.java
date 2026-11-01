package com.hrms.leave.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("leave_balances")
@CompoundIndex(name = "employee_leave_type_year_unique", def = "{'employeeId':1,'leaveType':1,'year':1}", unique = true)
public class LeaveBalance {
    @Id private String id;
    private String employeeId;
    private LeaveType leaveType;
    private int year;
    private BigDecimal allocatedDays = BigDecimal.ZERO;
    private BigDecimal usedDays = BigDecimal.ZERO;
    private BigDecimal pendingDays = BigDecimal.ZERO;
    private BigDecimal availableDays = BigDecimal.ZERO;
    private String allocatedBy;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public LeaveType getLeaveType() { return leaveType; } public void setLeaveType(LeaveType v) { leaveType = v; }
    public int getYear() { return year; } public void setYear(int v) { year = v; }
    public BigDecimal getAllocatedDays() { return allocatedDays; } public void setAllocatedDays(BigDecimal v) { allocatedDays = v; }
    public BigDecimal getUsedDays() { return usedDays; } public void setUsedDays(BigDecimal v) { usedDays = v; }
    public BigDecimal getPendingDays() { return pendingDays; } public void setPendingDays(BigDecimal v) { pendingDays = v; }
    public BigDecimal getAvailableDays() { return availableDays; } public void setAvailableDays(BigDecimal v) { availableDays = v; }
    public String getAllocatedBy() { return allocatedBy; } public void setAllocatedBy(String v) { allocatedBy = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
