package com.hrms.department.repository;

import com.hrms.department.model.DepartmentStatus;

public record DepartmentSearch(String search, DepartmentStatus status, int page, int limit, String sortBy, String sortDirection) { }
