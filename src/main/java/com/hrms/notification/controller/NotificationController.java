package com.hrms.notification.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.notification.dto.*;
import com.hrms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Employee notifications")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class NotificationController {
    private final NotificationService service; private final ClockSource clock;
    public NotificationController(NotificationService service, ClockSource clock) { this.service = service; this.clock = clock; }

    @GetMapping
    public ApiResponse<NotificationPageResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit, @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Notifications retrieved", service.list(principal, page, limit), clock);
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Unread notification count retrieved", service.unreadCount(principal), clock);
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable String notificationId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Notification marked as read", service.markRead(notificationId, principal), clock);
    }

    @PatchMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("All notifications marked as read", service.markAllRead(principal), clock);
    }
}
