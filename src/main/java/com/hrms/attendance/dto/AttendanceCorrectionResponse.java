package com.hrms.attendance.dto;

import com.hrms.attendance.model.*;
import java.time.*;

public record AttendanceCorrectionResponse(String id, String employeeId, String employeeCode, String employeeName,
        String departmentId, LocalDate attendanceDate, MissingPunchType missingPunch, Instant requestedCheckInAt,
        Instant requestedCheckOutAt, String reason, AttendanceCorrectionStatus status, String adminResponse,
        String reviewedBy, Instant reviewedAt, AttendanceResponse existingAttendance, ShiftSnapshot shift,
        Instant createdAt, Instant updatedAt) {
    public static AttendanceCorrectionResponse from(AttendanceCorrectionRequestEntity r, AttendanceResponse attendance, ShiftSnapshot shift) {
        return new AttendanceCorrectionResponse(r.getId(), r.getEmployeeId(), r.getEmployeeCodeSnapshot(), r.getEmployeeNameSnapshot(),
                r.getDepartmentIdSnapshot(), r.getAttendanceDate(), r.getMissingPunch(), r.getRequestedCheckInAt(), r.getRequestedCheckOutAt(),
                r.getReason(), r.getStatus(), r.getAdminResponse(), r.getReviewedBy(), r.getReviewedAt(), attendance, shift, r.getCreatedAt(), r.getUpdatedAt());
    }
}
