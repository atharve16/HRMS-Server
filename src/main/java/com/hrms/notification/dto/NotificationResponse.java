package com.hrms.notification.dto;

import com.hrms.notification.model.*;
import java.time.Instant;

public record NotificationResponse(String id, String title, String message, NotificationType type,
                                   String referenceId, boolean isRead, Instant readAt, Instant createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.getReferenceId(),
                n.isRead(), n.getReadAt(), n.getCreatedAt());
    }
}
