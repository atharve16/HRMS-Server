package com.hrms.department.repository;

import com.hrms.department.model.Department;
import org.springframework.data.domain.Page;

public interface DepartmentRepositoryCustom { Page<Department> search(DepartmentSearch search); }
