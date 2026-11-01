package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.math.BigDecimal;
import java.time.*;

public record LeaveRequestResponse(String id, String employeeId, String employeeCode, LeaveType leaveType,
                                   String leaveTypeName, LocalDate startDate, LocalDate endDate,
                                   BigDecimal totalDays, String reason, LeaveStatus status, boolean paid,
                                   String adminRemarks, String reviewedBy, Instant reviewedAt,
                                   Instant createdAt, Instant updatedAt) {
    public static LeaveRequestResponse from(LeaveRequest r) {
        return new LeaveRequestResponse(r.getId(), r.getEmployeeId(), r.getEmployeeCodeSnapshot(),
                r.getLeaveType(), r.getLeaveType().getDisplayName(), r.getStartDate(), r.getEndDate(),
                r.getTotalDays(), r.getReason(), r.getStatus(), r.isPaid(), r.getAdminRemarks(),
                r.getReviewedBy(), r.getReviewedAt(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
