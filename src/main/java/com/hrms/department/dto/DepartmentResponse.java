package com.hrms.department.dto;

import com.hrms.department.model.Department;
import com.hrms.department.model.DepartmentStatus;
import java.time.Instant;

public record DepartmentResponse(String id, String departmentCode, String name, String description,
                                 String managerEmployeeId, DepartmentStatus status,
                                 Instant createdAt, Instant updatedAt) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getDepartmentCode(), d.getName(), d.getDescription(),
                d.getManagerEmployeeId(), d.getStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
