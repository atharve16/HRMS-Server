package com.hrms.employee.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record EmployeePageResponse(
        List<EmployeeResponse> content,
        int page,
        int limit,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static EmployeePageResponse from(Page<com.hrms.employee.model.Employee> page, int limit) {
        return new EmployeePageResponse(page.getContent().stream().map(EmployeeResponse::from).toList(),
                page.getNumber(), limit, page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }
}
