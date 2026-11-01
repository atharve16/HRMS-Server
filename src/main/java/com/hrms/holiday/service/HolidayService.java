package com.hrms.holiday.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.holiday.dto.*;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.notification.service.NotificationService;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class HolidayService {
    private final CompanyHolidayRepository holidays;
    private final EmployeeRepository employees;
    private final CompanyTimeProvider time;
    private final AuditService audit;
    private final NotificationService notifications;

    public HolidayService(CompanyHolidayRepository holidays, EmployeeRepository employees,
                          CompanyTimeProvider time, AuditService audit, NotificationService notifications) {
        this.holidays = holidays;
        this.employees = employees;
        this.time = time;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional
    public AdminHolidayResponse create(CreateHolidayRequest input, AuthPrincipal admin) {
        String name = requiredName(input.name());
        if (holidays.existsByDateAndActiveTrue(input.date())) throw duplicate(input.date());
        CompanyHoliday holiday = new CompanyHoliday();
        holiday.setId(new ObjectId().toHexString());
        holiday.setName(name);
        holiday.setDescription(trim(input.description()));
        holiday.setDate(input.date());
        holiday.setActive(true);
        holiday.setCreatedBy(admin.userId());
        holiday.setUpdatedBy(admin.userId());
        try { holidays.save(holiday); }
        catch (DuplicateKeyException exception) { throw duplicate(input.date()); }
        audit.record(admin, "HOLIDAY_CREATED", "COMPANY_HOLIDAY", holiday.getId(), Map.of(), auditValues(holiday), null);
        notifications.holidayDeclared(holiday, admin.userId());
        return AdminHolidayResponse.from(holiday);
    }

    public HolidayPageResponse list(HolidaySearch search) {
        validatePage(search.page(), search.limit());
        DateRange range = range(search.year(), search.fromDate(), search.toDate(), false);
        return HolidayPageResponse.from(holidays.search(new HolidaySearch(null, range.from(), range.to(),
                search.active(), search.page(), search.limit(), search.sortDirection())), search.limit());
    }

    public AdminHolidayResponse detail(String holidayId) { return AdminHolidayResponse.from(find(holidayId)); }

    @Transactional
    public AdminHolidayResponse update(String holidayId, UpdateHolidayRequest input, AuthPrincipal admin) {
        if (input.name() == null && input.description() == null && input.date() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "At least one holiday field is required");
        }
        CompanyHoliday holiday = find(holidayId);
        String newName = input.name() == null ? holiday.getName() : requiredName(input.name());
        String newDescription = input.description() == null ? holiday.getDescription() : trim(input.description());
        LocalDate newDate = input.date() == null ? holiday.getDate() : input.date();
        if (holiday.isActive() && !newDate.equals(holiday.getDate())
                && holidays.existsByDateAndActiveTrueAndIdNot(newDate, holiday.getId())) throw duplicate(newDate);
        if (Objects.equals(newName, holiday.getName()) && Objects.equals(newDescription, holiday.getDescription())
                && Objects.equals(newDate, holiday.getDate())) return AdminHolidayResponse.from(holiday);

        Map<String, Object> before = auditValues(holiday);
        holiday.setName(newName);
        holiday.setDescription(newDescription);
        holiday.setDate(newDate);
        holiday.setUpdatedBy(admin.userId());
        try { holidays.save(holiday); }
        catch (DuplicateKeyException exception) { throw duplicate(newDate); }
        audit.record(admin, "HOLIDAY_UPDATED", "COMPANY_HOLIDAY", holiday.getId(), before, auditValues(holiday), null);
        if (holiday.isActive()) notifications.holidayUpdated(holiday, admin.userId());
        return AdminHolidayResponse.from(holiday);
    }

    @Transactional
    public AdminHolidayResponse deactivate(String holidayId, AuthPrincipal admin) {
        CompanyHoliday holiday = find(holidayId);
        if (!holiday.isActive()) return AdminHolidayResponse.from(holiday);
        Map<String, Object> before = auditValues(holiday);
        holiday.setActive(false);
        holiday.setUpdatedBy(admin.userId());
        holidays.save(holiday);
        audit.record(admin, "HOLIDAY_DEACTIVATED", "COMPANY_HOLIDAY", holiday.getId(), before,
                auditValues(holiday), null);
        notifications.holidayDeactivated(holiday, admin.userId());
        return AdminHolidayResponse.from(holiday);
    }

    public List<EmployeeHolidayResponse> employeeList(Integer year, LocalDate fromDate, LocalDate toDate,
                                                       AuthPrincipal principal) {
        validateEmployee(principal);
        DateRange range = range(year, fromDate, toDate, true);
        LocalDate from = range.from() == null ? LocalDate.of(2000, 1, 1) : range.from();
        LocalDate to = range.to() == null ? LocalDate.of(2200, 12, 31) : range.to();
        return holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(from, to).stream()
                .map(EmployeeHolidayResponse::from).toList();
    }

    public List<EmployeeHolidayResponse> upcoming(AuthPrincipal principal) {
        validateEmployee(principal);
        return holidays.findByActiveTrueAndDateGreaterThanOrderByDateAsc(time.companyDate()).stream()
                .map(EmployeeHolidayResponse::from).toList();
    }

    private void validateEmployee(AuthPrincipal principal) {
        employees.findByUserId(principal.userId()).filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.FORBIDDEN,
                        "Employee account is inactive or unavailable"));
    }

    private CompanyHoliday find(String id) {
        if (!ObjectId.isValid(id)) throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "Holiday id is invalid");
        return holidays.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company holiday was not found"));
    }

    private DateRange range(Integer year, LocalDate fromDate, LocalDate toDate, boolean defaultCurrentYear) {
        if (year != null && (year < 2000 || year > 2200))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Year is invalid");
        Integer effectiveYear = year == null && defaultCurrentYear && fromDate == null && toDate == null
                ? time.companyDate().getYear() : year;
        LocalDate from = fromDate;
        LocalDate to = toDate;
        if (effectiveYear != null) {
            LocalDate yearStart = LocalDate.of(effectiveYear, 1, 1);
            LocalDate yearEnd = LocalDate.of(effectiveYear, 12, 31);
            from = from == null || from.isBefore(yearStart) ? yearStart : from;
            to = to == null || to.isAfter(yearEnd) ? yearEnd : to;
        }
        if (from != null && to != null && to.isBefore(from))
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "toDate cannot be earlier than fromDate or outside the selected year");
        return new DateRange(from, to);
    }

    private static Map<String, Object> auditValues(CompanyHoliday h) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", h.getName());
        values.put("date", h.getDate().toString());
        values.put("active", h.isActive());
        if (h.getDescription() != null) values.put("description", h.getDescription());
        return values;
    }
    private static String requiredName(String value) {
        if (value == null || value.isBlank()) throw new ApiException(ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST, "Holiday name is required");
        return value.strip();
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static ApiException duplicate(LocalDate date) {
        return new ApiException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                "An active company holiday already exists for " + date);
    }
    private static void validatePage(int page, int limit) {
        if (page < 0 || limit < 1 || limit > 100) throw new ApiException(ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST, "Page must be non-negative and limit must be between 1 and 100");
    }
    private record DateRange(LocalDate from, LocalDate to) {}
}
