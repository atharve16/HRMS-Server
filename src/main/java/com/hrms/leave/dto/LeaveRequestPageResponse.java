package com.hrms.leave.dto;

import com.hrms.leave.model.LeaveRequest;
import org.springframework.data.domain.Page;
import java.util.List;

public record LeaveRequestPageResponse(List<LeaveRequestResponse> content, int page, int limit,
                                       long totalElements, int totalPages, boolean first, boolean last) {
    public static LeaveRequestPageResponse from(Page<LeaveRequest> page, int limit) {
        return new LeaveRequestPageResponse(page.getContent().stream().map(LeaveRequestResponse::from).toList(),
                page.getNumber(), limit, page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
