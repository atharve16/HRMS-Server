package com.hrms.notification;

import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ResourceNotFoundException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.leave.model.*;
import com.hrms.notification.model.Notification;
import com.hrms.notification.model.NotificationType;
import com.hrms.notification.repository.NotificationRepository;
import com.hrms.notification.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final String USER_ID = "507f1f77bcf86cd799439001";
    private static final String EMPLOYEE_ID = "507f1f77bcf86cd799439002";
    private static final String NOTIFICATION_ID = "507f1f77bcf86cd799439003";
    private final AuthPrincipal principal = new AuthPrincipal(USER_ID, "e@example.com", UserRole.EMPLOYEE);
    @Mock NotificationRepository notifications;
    @Mock EmployeeRepository employees;
    @Mock CompanyTimeProvider time;
    @Mock NotificationEmailService email;
    NotificationService service;
    Employee employee;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications, employees, time, email);
        employee = employee(EMPLOYEE_ID, USER_ID, "e@example.com", EmploymentStatus.ACTIVE);
        lenient().when(employees.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        lenient().when(time.now()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));
        lenient().when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listsOnlyAuthenticatedUsersNotificationsAndCountsUnread() {
        when(notifications.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(notifications.countByUserIdAndReadFalse(USER_ID)).thenReturn(4L);
        assertThat(service.list(principal, 0, 20).totalElements()).isZero();
        assertThat(service.unreadCount(principal).unreadCount()).isEqualTo(4);
        verify(notifications).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    @Test
    void markReadIsOwnedAndIdempotentAndReadAllIsScoped() {
        Notification notification = notification(false);
        when(notifications.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.of(notification));
        assertThat(service.markRead(NOTIFICATION_ID, principal).isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(time.now());
        service.markRead(NOTIFICATION_ID, principal);
        verify(notifications, times(1)).save(notification);

        service.markAllRead(principal);
        verify(notifications).markAllRead(USER_ID, time.now());
        when(notifications.findByIdAndUserId(NOTIFICATION_ID, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(NOTIFICATION_ID, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void leaveApprovalAndRejectionNotifyOnlyActiveSubmittingEmployee() {
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        LeaveRequest request = leaveRequest();
        service.leaveApproved(request);
        request.setAdminRemarks("Coverage unavailable");
        service.leaveRejected(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getUserId).containsOnly(USER_ID);
        assertThat(captor.getAllValues()).extracting(Notification::getType)
                .containsExactly(NotificationType.LEAVE_APPROVED, NotificationType.LEAVE_REJECTED);
        verify(email).sendLeaveDecision("e@example.com", true, "CL", request.getStartDate(), request.getEndDate(), null);
        verify(email).sendLeaveDecision("e@example.com", false, "CL", request.getStartDate(), request.getEndDate(),
                "Coverage unavailable");
    }

    @Test
    void holidayAnnouncementTargetsActiveEmployeesAndSkipsActor() {
        Employee other = employee("507f1f77bcf86cd799439010", "other-user", "other@example.com", EmploymentStatus.ACTIVE);
        Employee inactive = employee("507f1f77bcf86cd799439011", "inactive-user", "inactive@example.com", EmploymentStatus.INACTIVE);
        when(employees.findByEmploymentStatus(EmploymentStatus.ACTIVE)).thenReturn(List.of(employee, other));
        CompanyHoliday holiday = new CompanyHoliday(); holiday.setId("507f1f77bcf86cd799439020");
        holiday.setName("Republic Day"); holiday.setDate(LocalDate.of(2030, 1, 26)); holiday.setActive(true);

        service.holidayDeclared(holiday, USER_ID);

        @SuppressWarnings("unchecked") ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notifications).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Notification::getUserId).containsExactly("other-user");
        verify(email).sendHolidayAnnouncement("other@example.com", "Republic Day", holiday.getDate());
        verify(email, never()).sendHolidayAnnouncement(eq(inactive.getEmail()), anyString(), any());
    }

    private static Employee employee(String id, String userId, String email, EmploymentStatus status) {
        Employee employee = new Employee(); employee.setId(id); employee.setUserId(userId); employee.setEmail(email);
        employee.setEmploymentStatus(status); return employee;
    }
    private static Notification notification(boolean read) {
        Notification n = new Notification(); n.setId(NOTIFICATION_ID); n.setUserId(USER_ID); n.setTitle("Title");
        n.setMessage("Message"); n.setType(NotificationType.HOLIDAY_ANNOUNCEMENT); n.setRead(read); return n;
    }
    private static LeaveRequest leaveRequest() {
        LeaveRequest r = new LeaveRequest(); r.setId("507f1f77bcf86cd799439004"); r.setEmployeeId(EMPLOYEE_ID);
        r.setLeaveType(LeaveType.CL); r.setStartDate(LocalDate.of(2030, 1, 7)); r.setEndDate(LocalDate.of(2030, 1, 8));
        return r;
    }
}
