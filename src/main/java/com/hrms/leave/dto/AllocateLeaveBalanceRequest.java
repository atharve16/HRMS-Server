package com.hrms.leave.dto;

import com.hrms.leave.model.LeaveType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AllocateLeaveBalanceRequest(
        @NotNull LeaveType leaveType,
        @Min(2000) @Max(2200) int year,
        @NotNull @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal allocatedDays
) {}
