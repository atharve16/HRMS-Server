package com.hrms.holiday;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.dto.*;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.holiday.service.HolidayService;
import com.hrms.notification.service.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {
    private static final String HOLIDAY_ID = "507f1f77bcf86cd799439011";
    private final AuthPrincipal admin = new AuthPrincipal("admin", "admin@example.com", UserRole.ADMIN);
    private final AuthPrincipal employeePrincipal = new AuthPrincipal("employee-user", "e@example.com", UserRole.EMPLOYEE);
    @Mock CompanyHolidayRepository holidays;
    @Mock EmployeeRepository employees;
    @Mock CompanyTimeProvider time;
    @Mock AuditService audit;
    @Mock NotificationService notifications;
    HolidayService service;

    @BeforeEach
    void setUp() {
        service = new HolidayService(holidays, employees, time, audit, notifications);
        lenient().when(holidays.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(time.companyDate()).thenReturn(LocalDate.of(2030, 1, 1));
    }

    @Test
    void createsPaidCompanyHolidayAndAnnouncesIt() {
        LocalDate date = LocalDate.of(2030, 1, 26);
        when(holidays.existsByDateAndActiveTrue(date)).thenReturn(false);

        var response = service.create(new CreateHolidayRequest(" Republic Day ", "National holiday", date), admin);

        assertThat(response.name()).isEqualTo("Republic Day");
        assertThat(response.active()).isTrue();
        ArgumentCaptor<CompanyHoliday> captor = ArgumentCaptor.forClass(CompanyHoliday.class);
        verify(holidays).save(captor.capture());
        verify(notifications).holidayDeclared(captor.getValue(), admin.userId());
        verify(audit).record(eq(admin), eq("HOLIDAY_CREATED"), eq("COMPANY_HOLIDAY"), anyString(), anyMap(), anyMap(), isNull());
    }

    @Test
    void rejectsDuplicateActiveHolidayDate() {
        LocalDate date = LocalDate.of(2030, 1, 26);
        when(holidays.existsByDateAndActiveTrue(date)).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateHolidayRequest("Republic Day", null, date), admin))
                .isInstanceOf(ApiException.class).hasMessageContaining("already exists");
        verify(holidays, never()).save(any());
    }

    @Test
    void updatesActiveHolidayAndDeactivatesWithoutDeleting() {
        CompanyHoliday holiday = holiday(LocalDate.of(2030, 1, 26), true);
        when(holidays.findById(HOLIDAY_ID)).thenReturn(Optional.of(holiday));
        LocalDate newDate = LocalDate.of(2030, 1, 27);
        when(holidays.existsByDateAndActiveTrueAndIdNot(newDate, HOLIDAY_ID)).thenReturn(false);

        var updated = service.update(HOLIDAY_ID, new UpdateHolidayRequest("Observed Republic Day", null, newDate), admin);
        assertThat(updated.date()).isEqualTo(newDate);
        verify(notifications).holidayUpdated(holiday, admin.userId());

        var deactivated = service.deactivate(HOLIDAY_ID, admin);
        assertThat(deactivated.active()).isFalse();
        verify(notifications).holidayDeactivated(holiday, admin.userId());
        verify(holidays, never()).delete(any());
    }

    @Test
    void employeeSeesOnlyRepositorySuppliedActiveCalendarAndUpcomingOrder() {
        Employee employee = new Employee(); employee.setUserId(employeePrincipal.userId());
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        when(employees.findByUserId(employeePrincipal.userId())).thenReturn(Optional.of(employee));
        CompanyHoliday january = holiday(LocalDate.of(2030, 1, 26), true);
        CompanyHoliday august = holiday(LocalDate.of(2030, 8, 15), true);
        when(holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31))).thenReturn(List.of(january, august));
        when(holidays.findByActiveTrueAndDateGreaterThanOrderByDateAsc(LocalDate.of(2030, 1, 1)))
                .thenReturn(List.of(january, august));

        assertThat(service.employeeList(2030, null, null, employeePrincipal))
                .extracting(EmployeeHolidayResponse::date).containsExactly(january.getDate(), august.getDate());
        assertThat(service.upcoming(employeePrincipal)).extracting(EmployeeHolidayResponse::date)
                .containsExactly(january.getDate(), august.getDate());
    }

    @Test
    void adminListUsesDatabasePaginationAndDateFilters() {
        when(holidays.search(any())).thenReturn(new PageImpl<>(List.of()));
        service.list(new HolidaySearch(2030, null, null, true, 1, 10, "desc"));
        ArgumentCaptor<HolidaySearch> captor = ArgumentCaptor.forClass(HolidaySearch.class);
        verify(holidays).search(captor.capture());
        assertThat(captor.getValue().fromDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(captor.getValue().toDate()).isEqualTo(LocalDate.of(2030, 12, 31));
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    private static CompanyHoliday holiday(LocalDate date, boolean active) {
        CompanyHoliday holiday = new CompanyHoliday(); holiday.setId(HOLIDAY_ID); holiday.setName("Republic Day");
        holiday.setDescription("Holiday"); holiday.setDate(date); holiday.setActive(active); return holiday;
    }
}
