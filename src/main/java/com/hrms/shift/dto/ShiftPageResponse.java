package com.hrms.shift.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record ShiftPageResponse(List<ShiftResponse> content, int page, int limit, long totalElements, int totalPages,
                                boolean first, boolean last) {
    public static ShiftPageResponse from(Page<com.hrms.shift.model.Shift> p, int limit) { return new ShiftPageResponse(
            p.getContent().stream().map(ShiftResponse::from).toList(), p.getNumber(), limit, p.getTotalElements(),
            p.getTotalPages(), p.isFirst(), p.isLast()); }
}
