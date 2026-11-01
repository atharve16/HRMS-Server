package com.hrms.holiday.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateHolidayRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull LocalDate date
) {}
