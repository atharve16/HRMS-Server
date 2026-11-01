package com.hrms.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,20}") String departmentCode,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        String managerEmployeeId) { }
