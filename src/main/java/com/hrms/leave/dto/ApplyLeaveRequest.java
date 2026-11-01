package com.hrms.leave.dto;

import com.hrms.leave.model.LeaveType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ApplyLeaveRequest(
        @NotNull LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank @Size(max = 500) String reason
) {}
