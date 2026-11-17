package com.hrms.leave.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("leave_ledger_entries")
@CompoundIndex(name = "leave_ledger_idempotency_unique", def = "{'idempotencyKey':1}", unique = true,
        partialFilter = "{'idempotencyKey':{$exists:true}}")
@CompoundIndex(name = "leave_ledger_employee_period", def = "{'employeeId':1,'period':1,'createdAt':-1}")
public class LeaveLedgerEntry {
    @Id private String id;
    private String employeeId;
    private LeaveType leaveType;
    private String period;
    private LeaveLedgerEntryType entryType;
    private BigDecimal days;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String actorId;
    private String reason;
    private String source;
    private String idempotencyKey;
    @CreatedDate private Instant createdAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public LeaveType getLeaveType() { return leaveType; } public void setLeaveType(LeaveType v) { leaveType = v; }
    public String getPeriod() { return period; } public void setPeriod(String v) { period = v; }
    public LeaveLedgerEntryType getEntryType() { return entryType; } public void setEntryType(LeaveLedgerEntryType v) { entryType = v; }
    public BigDecimal getDays() { return days; } public void setDays(BigDecimal v) { days = v; }
    public BigDecimal getBalanceBefore() { return balanceBefore; } public void setBalanceBefore(BigDecimal v) { balanceBefore = v; }
    public BigDecimal getBalanceAfter() { return balanceAfter; } public void setBalanceAfter(BigDecimal v) { balanceAfter = v; }
    public String getActorId() { return actorId; } public void setActorId(String v) { actorId = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public String getSource() { return source; } public void setSource(String v) { source = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
