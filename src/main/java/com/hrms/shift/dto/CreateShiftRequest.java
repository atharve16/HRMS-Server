package com.hrms.shift.dto;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CreateShiftRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,20}") String shiftCode,
        @NotBlank @Size(max = 120) String name, @Size(max = 1000) String description,
        @NotNull LocalTime startTime, @NotNull LocalTime endTime,
        @NotEmpty Set<DayOfWeek> workingDays,
        @Min(0) @Max(1440) int gracePeriodMinutes,
        @Min(0) @Max(1440) int earlyDepartureGraceMinutes,
        @Min(0) @Max(1440) int expectedBreakMinutes,
        @Min(0) @Max(1440) Integer minimumBreakMinutes, boolean flexible) { }
