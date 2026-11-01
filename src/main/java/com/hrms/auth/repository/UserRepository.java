package com.hrms.auth.repository;

import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    boolean existsByRole(UserRole role);
}
