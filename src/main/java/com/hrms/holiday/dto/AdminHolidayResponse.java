package com.hrms.holiday.dto;

import com.hrms.holiday.model.CompanyHoliday;
import java.time.*;

public record AdminHolidayResponse(String id, String name, String description, LocalDate date, boolean active,
                                   String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
    public static AdminHolidayResponse from(CompanyHoliday h) {
        return new AdminHolidayResponse(h.getId(), h.getName(), h.getDescription(), h.getDate(), h.isActive(),
                h.getCreatedBy(), h.getUpdatedBy(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
