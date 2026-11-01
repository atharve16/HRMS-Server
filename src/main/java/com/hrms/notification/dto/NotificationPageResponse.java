package com.hrms.notification.dto;

import com.hrms.notification.model.Notification;
import org.springframework.data.domain.Page;
import java.util.List;

public record NotificationPageResponse(List<NotificationResponse> content, int page, int limit,
                                       long totalElements, int totalPages, boolean first, boolean last) {
    public static NotificationPageResponse from(Page<Notification> page, int limit) {
        return new NotificationPageResponse(page.getContent().stream().map(NotificationResponse::from).toList(),
                page.getNumber(), limit, page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
