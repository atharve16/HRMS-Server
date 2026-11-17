package com.hrms.leave.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/** Month-scoped balance used by configured monthly entitlements and rollover. */
@Document("leave_period_balances")
@CompoundIndex(name = "employee_leave_period_unique", def = "{'employeeId':1,'leaveType':1,'period':1}", unique = true)
public class LeavePeriodBalance {
    @Id private String id;
    private String employeeId;
    private LeaveType leaveType;
    /** ISO YearMonth, for example 2026-11. */
    private String period;
    private BigDecimal allocatedDays = BigDecimal.ZERO;
    private BigDecimal carriedForwardDays = BigDecimal.ZERO;
    private BigDecimal expiredDays = BigDecimal.ZERO;
    private BigDecimal usedDays = BigDecimal.ZERO;
    private BigDecimal pendingDays = BigDecimal.ZERO;
    private BigDecimal availableDays = BigDecimal.ZERO;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public LeaveType getLeaveType() { return leaveType; } public void setLeaveType(LeaveType v) { leaveType = v; }
    public String getPeriod() { return period; } public void setPeriod(String v) { period = v; }
    public BigDecimal getAllocatedDays() { return allocatedDays; } public void setAllocatedDays(BigDecimal v) { allocatedDays = v; }
    public BigDecimal getCarriedForwardDays() { return carriedForwardDays; } public void setCarriedForwardDays(BigDecimal v) { carriedForwardDays = v; }
    public BigDecimal getExpiredDays() { return expiredDays; } public void setExpiredDays(BigDecimal v) { expiredDays = v; }
    public BigDecimal getUsedDays() { return usedDays; } public void setUsedDays(BigDecimal v) { usedDays = v; }
    public BigDecimal getPendingDays() { return pendingDays; } public void setPendingDays(BigDecimal v) { pendingDays = v; }
    public BigDecimal getAvailableDays() { return availableDays; } public void setAvailableDays(BigDecimal v) { availableDays = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
