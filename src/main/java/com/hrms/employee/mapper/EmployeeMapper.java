package com.hrms.employee.mapper;

import com.hrms.employee.dto.AddressRequest;
import com.hrms.employee.dto.CreateEmployeeRequest;
import com.hrms.employee.dto.EmergencyContactRequest;
import com.hrms.employee.dto.UpdateEmployeeRequest;
import com.hrms.employee.model.Address;
import com.hrms.employee.model.EmergencyContact;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;

public final class EmployeeMapper {
    private EmployeeMapper() { }

    public static Employee from(CreateEmployeeRequest request) {
        Employee employee = new Employee();
        employee.setFirstName(request.firstName().strip());
        employee.setLastName(request.lastName().strip());
        employee.setPhone(normalizeNullable(request.phone()));
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setGender(request.gender());
        employee.setProfileImageUrl(normalizeNullable(request.profileImageUrl()));
        employee.setAddress(address(request.address()));
        employee.setDesignation(normalizeNullable(request.designation()));
        employee.setDepartmentId(normalizeNullable(request.departmentId()));
        employee.setEmploymentType(request.employmentType());
        employee.setJoiningDate(request.joiningDate());
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setReportingManagerId(normalizeNullable(request.reportingManagerId()));
        employee.setWorkLocation(normalizeNullable(request.workLocation()));
        employee.setEmergencyContact(emergencyContact(request.emergencyContact()));
        return employee;
    }

    public static void apply(UpdateEmployeeRequest request, Employee employee) {
        if (request.firstName() != null) employee.setFirstName(request.firstName().strip());
        if (request.lastName() != null) employee.setLastName(request.lastName().strip());
        if (request.phone() != null) employee.setPhone(normalizeNullable(request.phone()));
        if (request.dateOfBirth() != null) employee.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) employee.setGender(request.gender());
        if (request.profileImageUrl() != null) employee.setProfileImageUrl(normalizeNullable(request.profileImageUrl()));
        if (request.address() != null) employee.setAddress(address(request.address()));
        if (request.designation() != null) employee.setDesignation(normalizeNullable(request.designation()));
        if (request.departmentId() != null) employee.setDepartmentId(normalizeNullable(request.departmentId()));
        if (request.employmentType() != null) employee.setEmploymentType(request.employmentType());
        if (request.reportingManagerId() != null) employee.setReportingManagerId(normalizeNullable(request.reportingManagerId()));
        if (request.workLocation() != null) employee.setWorkLocation(normalizeNullable(request.workLocation()));
        if (request.emergencyContact() != null) employee.setEmergencyContact(emergencyContact(request.emergencyContact()));
        if (request.terminationDate() != null) employee.setTerminationDate(request.terminationDate());
    }

    private static Address address(AddressRequest request) {
        if (request == null) return null;
        Address address = new Address();
        address.setAddressLine1(request.addressLine1().strip());
        address.setAddressLine2(normalizeNullable(request.addressLine2()));
        address.setCity(request.city().strip());
        address.setState(request.state().strip());
        address.setPostalCode(request.postalCode().strip());
        address.setCountry(request.country().strip());
        return address;
    }

    private static EmergencyContact emergencyContact(EmergencyContactRequest request) {
        if (request == null) return null;
        EmergencyContact contact = new EmergencyContact();
        contact.setName(request.name().strip());
        contact.setRelationship(request.relationship().strip());
        contact.setPhone(request.phone().strip());
        return contact;
    }

    public static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }
}
