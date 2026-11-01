package com.hrms.employee.repository;

import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.model.EmploymentType;

public record EmployeeSearch(String search, EmploymentStatus employmentStatus,
                             EmploymentType employmentType, String departmentId,
                             String designation, int page, int limit,
                             String sortBy, String sortDirection) {
}
