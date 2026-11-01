package com.hrms.employee.repository;

import com.hrms.employee.model.Employee;
import org.springframework.data.domain.Page;

public interface EmployeeRepositoryCustom {
    Page<Employee> search(EmployeeSearch search);
}
