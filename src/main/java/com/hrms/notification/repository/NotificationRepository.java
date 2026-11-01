package com.hrms.notification.repository;

import com.hrms.notification.model.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String>, NotificationRepositoryCustom {
    Page<Notification> findByUserId(String userId, Pageable pageable);
    long countByUserIdAndReadFalse(String userId);
    Optional<Notification> findByIdAndUserId(String id, String userId);
}
