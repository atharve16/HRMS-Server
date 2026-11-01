package com.hrms.notification.service;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.leave.model.LeaveRequest;
import com.hrms.notification.dto.*;
import com.hrms.notification.model.*;
import com.hrms.notification.repository.NotificationRepository;
import org.bson.types.ObjectId;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;

import java.util.*;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications;
    private final EmployeeRepository employees;
    private final CompanyTimeProvider time;
    private final NotificationEmailService email;

    public NotificationService(NotificationRepository notifications, EmployeeRepository employees,
                               CompanyTimeProvider time, NotificationEmailService email) {
        this.notifications = notifications;
        this.employees = employees;
        this.time = time;
        this.email = email;
    }

    public void holidayDeclared(CompanyHoliday holiday, String actorUserId) {
        List<Employee> recipients = activeRecipients(actorUserId);
        saveHolidayNotifications(recipients, holiday, "Company holiday announced",
                "%s on %s has been declared a paid company holiday.".formatted(holiday.getName(), holiday.getDate()));
        afterCommit(() -> recipients.forEach(employee ->
                email.sendHolidayAnnouncement(employee.getEmail(), holiday.getName(), holiday.getDate())));
    }

    public void holidayUpdated(CompanyHoliday holiday, String actorUserId) {
        saveHolidayNotifications(activeRecipients(actorUserId), holiday, "Company holiday updated",
                "The company holiday %s is scheduled for %s.".formatted(holiday.getName(), holiday.getDate()));
    }

    public void holidayDeactivated(CompanyHoliday holiday, String actorUserId) {
        saveHolidayNotifications(activeRecipients(actorUserId), holiday, "Company holiday deactivated",
                "%s on %s is no longer an active company holiday.".formatted(holiday.getName(), holiday.getDate()));
    }

    public void leaveApproved(LeaveRequest request) { leaveDecision(request, true); }
    public void leaveRejected(LeaveRequest request) { leaveDecision(request, false); }

    public NotificationPageResponse list(AuthPrincipal principal, int page, int limit) {
        Employee employee = activeEmployee(principal.userId());
        validatePage(page, limit);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return NotificationPageResponse.from(notifications.findByUserId(employee.getUserId(), pageable), limit);
    }

    public UnreadCountResponse unreadCount(AuthPrincipal principal) {
        Employee employee = activeEmployee(principal.userId());
        return new UnreadCountResponse(notifications.countByUserIdAndReadFalse(employee.getUserId()));
    }

    public NotificationResponse markRead(String notificationId, AuthPrincipal principal) {
        Employee employee = activeEmployee(principal.userId());
        if (!ObjectId.isValid(notificationId)) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "Notification id is invalid");
        Notification notification = notifications.findByIdAndUserId(notificationId, employee.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(time.now());
            notifications.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    public UnreadCountResponse markAllRead(AuthPrincipal principal) {
        Employee employee = activeEmployee(principal.userId());
        notifications.markAllRead(employee.getUserId(), time.now());
        return new UnreadCountResponse(0);
    }

    private void leaveDecision(LeaveRequest request, boolean approved) {
        Employee employee = employees.findById(request.getEmployeeId())
                .filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE).orElse(null);
        if (employee == null) return;
        String decision = approved ? "approved" : "rejected";
        Notification notification = notification(employee.getUserId(), "Leave request " + decision,
                "Your %s request from %s to %s was %s%s".formatted(request.getLeaveType(), request.getStartDate(),
                        request.getEndDate(), decision,
                        approved || request.getAdminRemarks() == null ? "." : ". Remarks: " + request.getAdminRemarks()),
                approved ? NotificationType.LEAVE_APPROVED : NotificationType.LEAVE_REJECTED, request.getId());
        notifications.save(notification);
        afterCommit(() -> email.sendLeaveDecision(employee.getEmail(), approved, request.getLeaveType().name(),
                request.getStartDate(), request.getEndDate(), request.getAdminRemarks()));
    }

    private void saveHolidayNotifications(List<Employee> recipients, CompanyHoliday holiday, String title, String message) {
        if (recipients.isEmpty()) return;
        notifications.saveAll(recipients.stream().map(employee -> notification(employee.getUserId(), title, message,
                NotificationType.HOLIDAY_ANNOUNCEMENT, holiday.getId())).toList());
    }

    private List<Employee> activeRecipients(String actorUserId) {
        return employees.findByEmploymentStatus(EmploymentStatus.ACTIVE).stream()
                .filter(employee -> employee.getUserId() != null && !employee.getUserId().equals(actorUserId))
                .toList();
    }

    private Employee activeEmployee(String userId) {
        return employees.findByUserId(userId).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN,
                        "Employee account is inactive or unavailable"));
    }

    private static Notification notification(String userId, String title, String message,
                                             NotificationType type, String referenceId) {
        Notification notification = new Notification();
        notification.setId(new ObjectId().toHexString());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        return notification;
    }

    private static void validatePage(int page, int limit) {
        if (page < 0 || limit < 1 || limit > 100) throw new ApiException(ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST, "Page must be non-negative and limit must be between 1 and 100");
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { runSafely(task); }
            });
        } else runSafely(task);
    }

    private static void runSafely(Runnable task) {
        try { task.run(); }
        catch (Exception exception) { log.error("Post-commit notification email failed", exception); }
    }
}
