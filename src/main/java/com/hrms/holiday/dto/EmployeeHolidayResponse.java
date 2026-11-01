package com.hrms.holiday.dto;

import com.hrms.holiday.model.CompanyHoliday;
import java.time.LocalDate;

public record EmployeeHolidayResponse(String id, String name, String description, LocalDate date) {
    public static EmployeeHolidayResponse from(CompanyHoliday h) {
        return new EmployeeHolidayResponse(h.getId(), h.getName(), h.getDescription(), h.getDate());
    }
}
