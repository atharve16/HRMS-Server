package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.math.BigDecimal;
import java.time.Instant;

public record LeaveLedgerEntryResponse(String id, String employeeId, LeaveType leaveType, String period,
                                       LeaveLedgerEntryType entryType, BigDecimal days, BigDecimal balanceBefore,
                                       BigDecimal balanceAfter, String actorId, String reason, String source,
                                       Instant createdAt) {
    public static LeaveLedgerEntryResponse from(LeaveLedgerEntry e) {
        return new LeaveLedgerEntryResponse(e.getId(), e.getEmployeeId(), e.getLeaveType(), e.getPeriod(),
                e.getEntryType(), e.getDays(), e.getBalanceBefore(), e.getBalanceAfter(), e.getActorId(),
                e.getReason(), e.getSource(), e.getCreatedAt());
    }
}
