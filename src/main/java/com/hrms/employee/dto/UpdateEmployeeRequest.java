package com.hrms.employee.dto;

import com.hrms.employee.model.EmploymentType;
import com.hrms.employee.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^$|\\+?[0-9]{10,15}") String phone,
        @Past LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 500) String profileImageUrl,
        @Valid AddressRequest address,
        @Size(max = 120) String designation,
        @Size(max = 100) String departmentId,
        EmploymentType employmentType,
        @Size(max = 100) String reportingManagerId,
        @Size(max = 150) String workLocation,
        @Valid EmergencyContactRequest emergencyContact,
        LocalDate terminationDate
) {
}
