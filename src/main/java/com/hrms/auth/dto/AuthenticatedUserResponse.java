package com.hrms.auth.dto;

import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthenticatedUserResponse(
        String id,
        @Schema(example = "employee@example.com") String email,
        UserRole role,
        UserStatus status,
        String employeeId
) {
    public static AuthenticatedUserResponse from(User user) {
        return new AuthenticatedUserResponse(
                user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getEmployeeId());
    }
}
