package com.hrms.assignment.dto;

import com.hrms.assignment.model.*;
import java.time.*;

public record AssignmentResponse(String id, String employeeId, String shiftId, String officeLocationId,
                                 LocalDate effectiveFrom, LocalDate effectiveTo, AssignmentStatus status,
                                 String reason, Instant createdAt, Instant updatedAt) {
    public static AssignmentResponse from(EmployeeWorkAssignment a) { return new AssignmentResponse(a.getId(), a.getEmployeeId(),
            a.getShiftId(), a.getOfficeLocationId(), a.getEffectiveFrom(), a.getEffectiveTo(), a.getStatus(), a.getReason(),
            a.getCreatedAt(), a.getUpdatedAt()); }
}
