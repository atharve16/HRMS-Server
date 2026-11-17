package com.hrms.attendance.dto;

import com.hrms.attendance.model.MissingPunchType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record SubmitAttendanceCorrectionRequest(
        @NotNull LocalDate attendanceDate,
        @NotNull MissingPunchType missingPunch,
        LocalTime requestedCheckInTime,
        LocalTime requestedCheckOutTime,
        @NotBlank @Size(max = 1000) String reason
) {}
