package com.hrms.employee;

import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.employee.dto.CreateEmployeeRequest;
import com.hrms.employee.dto.UpdateEmployeeRequest;
import com.hrms.employee.model.EmploymentType;

import java.time.LocalDate;

public final class EmployeeTestFixtures {
    private EmployeeTestFixtures() { }

    public static AuthPrincipal admin() {
        return new AuthPrincipal("admin-id", "admin@example.com", UserRole.ADMIN);
    }

    public static CreateEmployeeRequest create(String email) {
        return new CreateEmployeeRequest("Ada", "Lovelace", email, "+919876543210",
                LocalDate.of(1990, 1, 1), null, null, null, "Engineer", null,
                EmploymentType.FULL_TIME, LocalDate.of(2026, 1, 15), null, "HQ", null);
    }

    public static UpdateEmployeeRequest updateEmail(String email) {
        return new UpdateEmployeeRequest(null, null, email, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
