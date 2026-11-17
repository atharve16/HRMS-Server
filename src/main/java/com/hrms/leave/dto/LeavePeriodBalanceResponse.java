package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.math.BigDecimal;
import java.time.Instant;

public record LeavePeriodBalanceResponse(String id, String employeeId, LeaveType leaveType, String period,
                                         BigDecimal allocatedDays, BigDecimal carriedForwardDays,
                                         BigDecimal expiredDays, BigDecimal usedDays, BigDecimal pendingDays,
                                         BigDecimal availableDays, Instant updatedAt) {
    public static LeavePeriodBalanceResponse from(LeavePeriodBalance b) {
        return new LeavePeriodBalanceResponse(b.getId(), b.getEmployeeId(), b.getLeaveType(), b.getPeriod(),
                b.getAllocatedDays(), b.getCarriedForwardDays(), b.getExpiredDays(), b.getUsedDays(),
                b.getPendingDays(), b.getAvailableDays(), b.getUpdatedAt());
    }
}
