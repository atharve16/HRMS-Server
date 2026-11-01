package com.hrms.department.repository;

import com.hrms.department.model.Department;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepartmentRepository extends MongoRepository<Department, String>, DepartmentRepositoryCustom {
    Optional<Department> findByNormalizedName(String name);
    boolean existsByDepartmentCode(String code);
}
