package com.hrms.department.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record DepartmentPageResponse(List<DepartmentResponse> content, int page, int limit,
                                     long totalElements, int totalPages, boolean first, boolean last) {
    public static DepartmentPageResponse from(Page<com.hrms.department.model.Department> p, int limit) {
        return new DepartmentPageResponse(p.getContent().stream().map(DepartmentResponse::from).toList(),
                p.getNumber(), limit, p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }
}
