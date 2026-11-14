package com.hrms.employee.repository;

import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String>, EmployeeRepositoryCustom {
    Optional<Employee> findByUserId(String userId);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByDepartmentIdAndEmploymentStatus(String departmentId, EmploymentStatus status);
    List<Employee> findByEmploymentStatus(EmploymentStatus status);

    @Query("{'$or':[{'joiningDate':null},{'joiningDate':{'$lte':?0}}]}")
    List<Employee> findEmployedOnOrBefore(LocalDate date);
}
