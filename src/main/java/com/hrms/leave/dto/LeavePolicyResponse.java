package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.math.BigDecimal;
import java.time.Instant;

public record LeavePolicyResponse(String id, LeavePolicyScope scope, String employeeId, LeaveType leaveType,
                                  BigDecimal monthlyEntitlement, BigDecimal maxCarryForwardDays,
                                  Instant updatedAt) {
    public static LeavePolicyResponse from(LeavePolicy p) {
        return new LeavePolicyResponse(p.getId(), p.getScope(), p.getEmployeeId(), p.getLeaveType(),
                p.getMonthlyEntitlement(), p.getMaxCarryForwardDays(), p.getUpdatedAt());
    }
}
