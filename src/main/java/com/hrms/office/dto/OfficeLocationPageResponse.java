package com.hrms.office.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record OfficeLocationPageResponse(List<OfficeLocationResponse> content, int page, int limit,
                                         long totalElements, int totalPages, boolean first, boolean last) {
    public static OfficeLocationPageResponse from(Page<com.hrms.office.model.OfficeLocation> p, int limit) {
        return new OfficeLocationPageResponse(p.getContent().stream().map(OfficeLocationResponse::from).toList(),
                p.getNumber(), limit, p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }
}
