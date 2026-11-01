package com.hrms.holiday.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateHolidayRequest(
        @Size(min = 1, max = 120) String name,
        @Size(max = 500) String description,
        LocalDate date
) {}
