package com.hrms.leave.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Document("leave_requests")
@CompoundIndexes({
        @CompoundIndex(name = "employee_start_date_idx", def = "{'employeeId':1,'startDate':1}"),
        @CompoundIndex(name = "employee_status_idx", def = "{'employeeId':1,'status':1}"),
        @CompoundIndex(name = "status_start_date_idx", def = "{'status':1,'startDate':1}"),
        @CompoundIndex(name = "leave_type_status_idx", def = "{'leaveType':1,'status':1}"),
        @CompoundIndex(name = "active_employee_covered_date_unique", def = "{'employeeId':1,'coveredDates':1}",
                unique = true, partialFilter = "{'conflictActive':true}")
})
public class LeaveRequest {
    @Id private String id;
    private String employeeId;
    private String employeeCodeSnapshot;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private LeaveStatus status;
    private boolean paid;
    private String adminRemarks;
    private String reviewedBy;
    private Instant reviewedAt;
    private List<LocalDate> leaveDates = new ArrayList<>();
    private List<LocalDate> coveredDates = new ArrayList<>();
    private boolean conflictActive;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public String getEmployeeCodeSnapshot() { return employeeCodeSnapshot; } public void setEmployeeCodeSnapshot(String v) { employeeCodeSnapshot = v; }
    public LeaveType getLeaveType() { return leaveType; } public void setLeaveType(LeaveType v) { leaveType = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public BigDecimal getTotalDays() { return totalDays; } public void setTotalDays(BigDecimal v) { totalDays = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public LeaveStatus getStatus() { return status; } public void setStatus(LeaveStatus v) { status = v; }
    public boolean isPaid() { return paid; } public void setPaid(boolean v) { paid = v; }
    public String getAdminRemarks() { return adminRemarks; } public void setAdminRemarks(String v) { adminRemarks = v; }
    public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String v) { reviewedBy = v; }
    public Instant getReviewedAt() { return reviewedAt; } public void setReviewedAt(Instant v) { reviewedAt = v; }
    public List<LocalDate> getLeaveDates() { return leaveDates; } public void setLeaveDates(List<LocalDate> v) { leaveDates = v; }
    public List<LocalDate> getCoveredDates() { return coveredDates; } public void setCoveredDates(List<LocalDate> v) { coveredDates = v; }
    public boolean isConflictActive() { return conflictActive; } public void setConflictActive(boolean v) { conflictActive = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
