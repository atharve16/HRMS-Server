package com.hrms.employee.repository;

import com.hrms.employee.model.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.List;
import com.hrms.employee.model.EmploymentStatus;

public interface EmployeeRepository extends MongoRepository<Employee, String>, EmployeeRepositoryCustom {
    Optional<Employee> findByUserId(String userId);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByDepartmentIdAndEmploymentStatus(String departmentId, EmploymentStatus status);
    List<Employee> findByEmploymentStatus(EmploymentStatus status);
}
