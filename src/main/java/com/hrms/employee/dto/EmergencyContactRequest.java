package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80) String relationship,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}") String phone
) {
}
