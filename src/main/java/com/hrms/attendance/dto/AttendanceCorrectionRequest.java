package com.hrms.attendance.dto;
import jakarta.validation.constraints.*;import java.time.Instant;
public record AttendanceCorrectionRequest(@NotBlank String sessionId,@NotNull Instant correctedCheckInAt,Instant correctedCheckOutAt,@NotBlank @Size(max=1000) String reason) { }
