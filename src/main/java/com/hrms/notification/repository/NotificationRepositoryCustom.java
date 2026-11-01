package com.hrms.notification.repository;

import java.time.Instant;

public interface NotificationRepositoryCustom {
    long markAllRead(String userId, Instant readAt);
}
