package com.hrms.holiday.dto;

import com.hrms.holiday.model.CompanyHoliday;
import org.springframework.data.domain.Page;
import java.util.List;

public record HolidayPageResponse(List<AdminHolidayResponse> content, int page, int limit,
                                  long totalElements, int totalPages, boolean first, boolean last) {
    public static HolidayPageResponse from(Page<CompanyHoliday> page, int limit) {
        return new HolidayPageResponse(page.getContent().stream().map(AdminHolidayResponse::from).toList(),
                page.getNumber(), limit, page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
