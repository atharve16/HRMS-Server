package com.hrms.auth.repository;

import com.hrms.auth.entity.OtpVerification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends MongoRepository<OtpVerification, String> {
    Optional<OtpVerification> findTopByNormalizedEmailOrderByCreatedAtDesc(String normalizedEmail);
    Optional<OtpVerification> findTopByNormalizedEmailAndActiveTrueOrderByCreatedAtDesc(String normalizedEmail);
}
