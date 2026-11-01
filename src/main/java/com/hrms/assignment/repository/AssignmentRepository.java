package com.hrms.assignment.repository;

import com.hrms.assignment.model.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssignmentRepository extends MongoRepository<EmployeeWorkAssignment,String> {
    List<EmployeeWorkAssignment> findByEmployeeIdOrderByEffectiveFromDesc(String employeeId);
    List<EmployeeWorkAssignment> findByEmployeeIdAndStatus(String employeeId, AssignmentStatus status);
    boolean existsByShiftIdAndStatus(String shiftId, AssignmentStatus status);
    boolean existsByOfficeLocationIdAndStatus(String officeLocationId, AssignmentStatus status);
}
