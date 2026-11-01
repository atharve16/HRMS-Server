package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceResponse(String id, String employeeId, LeaveType leaveType, String leaveTypeName,
                                   int year, BigDecimal allocatedDays, BigDecimal usedDays,
                                   BigDecimal pendingDays, BigDecimal availableDays,
                                   Instant createdAt, Instant updatedAt) {
    public static LeaveBalanceResponse from(LeaveBalance b) {
        return new LeaveBalanceResponse(b.getId(), b.getEmployeeId(), b.getLeaveType(),
                b.getLeaveType().getDisplayName(), b.getYear(), b.getAllocatedDays(), b.getUsedDays(),
                b.getPendingDays(), b.getAvailableDays(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
