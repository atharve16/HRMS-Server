package com.hrms.leave.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("leave_rollover_records")
@CompoundIndex(name = "leave_rollover_employee_period_unique", def = "{'employeeId':1,'period':1}", unique = true)
public class LeaveRolloverRecord {
    @Id private String id;
    private String employeeId;
    private String period;
    private Instant processedAt;
    private String status;
    @CreatedDate private Instant createdAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public String getPeriod() { return period; } public void setPeriod(String v) { period = v; }
    public Instant getProcessedAt() { return processedAt; } public void setProcessedAt(Instant v) { processedAt = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
