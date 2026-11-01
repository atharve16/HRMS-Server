package com.hrms.department.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("departments")
public class Department {
    @Id private String id;
    @Indexed(unique = true) private String departmentCode;
    @Indexed(unique = true) private String normalizedName;
    private String name;
    private String description;
    private String managerEmployeeId;
    @Indexed private DepartmentStatus status;
    private String createdBy;
    private String updatedBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String value) { departmentCode = value; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String value) { normalizedName = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(String value) { managerEmployeeId = value; }
    public DepartmentStatus getStatus() { return status; }
    public void setStatus(DepartmentStatus value) { status = value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy = value; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String value) { updatedBy = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
