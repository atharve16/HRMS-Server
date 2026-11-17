package com.hrms.leave.dto;

import com.hrms.leave.model.LeaveType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LeavePolicyRequest(
        @NotNull LeaveType leaveType,
        @NotNull @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal monthlyEntitlement,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal maxCarryForwardDays
) {}
