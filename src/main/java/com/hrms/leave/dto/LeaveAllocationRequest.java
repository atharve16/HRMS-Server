package com.hrms.leave.dto;

import com.hrms.leave.model.LeaveType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LeaveAllocationRequest(
        @NotNull LeaveType leaveType,
        @NotBlank @Pattern(regexp = "20\\d{2}-(0[1-9]|1[0-2])") String month,
        @NotNull @Digits(integer = 4, fraction = 1) BigDecimal days,
        @NotBlank @Size(max = 500) String reason
) {}
