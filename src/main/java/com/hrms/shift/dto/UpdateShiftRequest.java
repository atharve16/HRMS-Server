package com.hrms.shift.dto;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateShiftRequest(@Pattern(regexp = "[A-Za-z0-9_-]{2,20}") String shiftCode,
        @Size(max = 120) String name, @Size(max = 1000) String description,
        LocalTime startTime, LocalTime endTime, Set<DayOfWeek> workingDays,
        @Min(0) @Max(1440) Integer gracePeriodMinutes,
        @Min(0) @Max(1440) Integer earlyDepartureGraceMinutes,
        @Min(0) @Max(1440) Integer expectedBreakMinutes,
        @Min(0) @Max(1440) Integer minimumBreakMinutes, Boolean flexible) { }
