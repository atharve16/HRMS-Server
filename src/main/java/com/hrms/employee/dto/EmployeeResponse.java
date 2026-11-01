package com.hrms.employee.dto;

import com.hrms.employee.model.Address;
import com.hrms.employee.model.EmergencyContact;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.model.EmploymentType;
import com.hrms.employee.model.Gender;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        String id,
        String employeeCode,
        String userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        String profileImageUrl,
        Address address,
        String designation,
        String departmentId,
        EmploymentType employmentType,
        LocalDate joiningDate,
        LocalDate terminationDate,
        EmploymentStatus employmentStatus,
        String reportingManagerId,
        String workLocation,
        EmergencyContact emergencyContact,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getEmployeeCode(), employee.getUserId(),
                employee.getFirstName(), employee.getLastName(), employee.getEmail(), employee.getPhone(),
                employee.getDateOfBirth(), employee.getGender(), employee.getProfileImageUrl(), employee.getAddress(),
                employee.getDesignation(), employee.getDepartmentId(), employee.getEmploymentType(),
                employee.getJoiningDate(), employee.getTerminationDate(), employee.getEmploymentStatus(), employee.getReportingManagerId(),
                employee.getWorkLocation(), employee.getEmergencyContact(), employee.getCreatedAt(), employee.getUpdatedAt());
    }
}
