package com.hrms.assignment.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document("employee_work_assignments")
public class EmployeeWorkAssignment {
    @Id private String id;
    @Indexed private String employeeId;
    @Indexed private String shiftId;
    @Indexed private String officeLocationId;
    @Indexed private LocalDate effectiveFrom;
    @Indexed private LocalDate effectiveTo;
    @Indexed private AssignmentStatus status;
    private String createdBy;
    private String reason;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id=v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId=v; }
    public String getShiftId() { return shiftId; } public void setShiftId(String v) { shiftId=v; }
    public String getOfficeLocationId() { return officeLocationId; } public void setOfficeLocationId(String v) { officeLocationId=v; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(LocalDate v) { effectiveFrom=v; }
    public LocalDate getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(LocalDate v) { effectiveTo=v; }
    public AssignmentStatus getStatus() { return status; } public void setStatus(AssignmentStatus v) { status=v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy=v; }
    public String getReason() { return reason; } public void setReason(String v) { reason=v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt=v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt=v; }
}
