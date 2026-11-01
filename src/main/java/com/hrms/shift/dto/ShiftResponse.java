package com.hrms.shift.dto;

import com.hrms.shift.model.Shift;
import com.hrms.shift.model.ShiftStatus;
import java.time.*;
import java.util.Set;

public record ShiftResponse(String id, String shiftCode, String name, String description, LocalTime startTime,
                            LocalTime endTime, Set<DayOfWeek> workingDays, int gracePeriodMinutes,
                            int earlyDepartureGraceMinutes, int expectedBreakMinutes, Integer minimumBreakMinutes,
                            boolean flexible, ShiftStatus status, Instant createdAt, Instant updatedAt) {
    public static ShiftResponse from(Shift s) { return new ShiftResponse(s.getId(), s.getShiftCode(), s.getName(), s.getDescription(),
            s.getStartTime(), s.getEndTime(), s.getWorkingDays(), s.getGracePeriodMinutes(), s.getEarlyDepartureGraceMinutes(),
            s.getExpectedBreakMinutes(), s.getMinimumBreakMinutes(), s.isFlexible(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt()); }
}
