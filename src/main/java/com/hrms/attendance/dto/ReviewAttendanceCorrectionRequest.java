package com.hrms.attendance.dto;

import jakarta.validation.constraints.Size;

public record ReviewAttendanceCorrectionRequest(@Size(max = 1000) String response) {}
