package com.hrms.phase4;

import com.hrms.assignment.dto.CreateAssignmentRequest;
import com.hrms.assignment.model.*;
import com.hrms.assignment.repository.AssignmentRepository;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ConflictException;
import com.hrms.department.dto.CreateDepartmentRequest;
import com.hrms.department.repository.DepartmentRepository;
import com.hrms.department.service.DepartmentService;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.office.dto.CoordinatesRequest;
import com.hrms.office.dto.CreateOfficeLocationRequest;
import com.hrms.office.repository.OfficeLocationRepository;
import com.hrms.office.service.OfficeLocationService;
import com.hrms.config.ApplicationProperties;
import com.hrms.shift.dto.CreateShiftRequest;
import com.hrms.shift.repository.ShiftRepository;
import com.hrms.shift.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase4ServiceTest {
    private final AuthPrincipal admin = new AuthPrincipal("admin", "admin@example.com", UserRole.ADMIN);

    @Mock DepartmentRepository departments;
    @Mock EmployeeRepository employees;
    @Mock OfficeLocationRepository offices;
    @Mock ShiftRepository shifts;
    @Mock AssignmentRepository assignments;
    @Mock AuditService audit;

    @Test
    void departmentRejectsInactiveManagerAndNormalizesName() {
        DepartmentService service = new DepartmentService(departments, employees, audit);
        Employee manager = new Employee(); manager.setId("507f1f77bcf86cd799439011"); manager.setEmploymentStatus(EmploymentStatus.INACTIVE);
        when(departments.existsByDepartmentCode("ENG")).thenReturn(false);
        when(departments.findByNormalizedName("engineering")).thenReturn(Optional.empty());
        when(employees.findById(manager.getId())).thenReturn(Optional.of(manager));
        assertThatThrownBy(() -> service.create(new CreateDepartmentRequest("eng", "Engineering", null, manager.getId()), admin))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void officeStoresGeoJsonAsLongitudeThenLatitude() {
        ApplicationProperties properties = mock(ApplicationProperties.class);
        ApplicationProperties.Geofence geofence = new ApplicationProperties.Geofence(100, 50);
        when(properties.geofence()).thenReturn(geofence);
        OfficeLocationService service = new OfficeLocationService(offices, assignments, properties, audit);
        when(offices.existsByLocationCode("HQ")).thenReturn(false);
        var response = service.create(new CreateOfficeLocationRequest("hq", "HQ", null, "1 Main", null,
                "Jabalpur", "MP", "482001", "India",
                new CoordinatesRequest(new BigDecimal("23.1815"), new BigDecimal("79.9864")), null, null), admin);
        var captor = org.mockito.ArgumentCaptor.forClass(com.hrms.office.model.OfficeLocation.class);
        verify(offices).save(captor.capture());
        assertThat(captor.getValue().getCoordinates().getX()).isEqualTo(79.9864);
        assertThat(captor.getValue().getCoordinates().getY()).isEqualTo(23.1815);
        assertThat(response.geofenceRadiusMeters()).isEqualTo(100);
    }

    @Test
    void equalShiftTimesAreAcceptedAsOvernightAndEmptyDaysRejected() {
        ShiftService service = new ShiftService(shifts, assignments, audit);
        when(shifts.existsByShiftCode("NIGHT")).thenReturn(false);
        var response = service.create(new CreateShiftRequest("night", "Night", null, LocalTime.of(22,0), LocalTime.of(22,0),
                EnumSet.of(DayOfWeek.MONDAY), 15, 10, 30, null, false), admin);
        assertThat(response.shiftCode()).isEqualTo("NIGHT");
        assertThatThrownBy(() -> service.create(new CreateShiftRequest("bad", "Bad", null, LocalTime.NOON, LocalTime.of(13,0),
                EnumSet.noneOf(DayOfWeek.class), 0, 0, 0, null, false), admin)).isInstanceOf(Exception.class);
    }

    @Test
    void assignmentRejectsOverlapAndResolvesEndedHistoricalAssignment() {
        ShiftService shiftService = mock(ShiftService.class); OfficeLocationService officeService = mock(OfficeLocationService.class);
        AssignmentService service = new AssignmentService(assignments, employees, shiftService, officeService, audit);
        String employeeId="507f1f77bcf86cd799439011"; Employee e=new Employee();e.setId(employeeId);e.setEmploymentStatus(EmploymentStatus.ACTIVE);
        when(employees.findById(employeeId)).thenReturn(Optional.of(e));
        EmployeeWorkAssignment old=new EmployeeWorkAssignment();old.setId("507f1f77bcf86cd799439012");old.setEmployeeId(employeeId);old.setEffectiveFrom(LocalDate.of(2026,1,1));old.setEffectiveTo(LocalDate.of(2026,1,31));old.setStatus(AssignmentStatus.INACTIVE);old.setShiftId("s");old.setOfficeLocationId("o");
        when(assignments.findByEmployeeIdAndStatus(employeeId, AssignmentStatus.ACTIVE)).thenReturn(List.of());
        when(assignments.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)).thenReturn(List.of(old));
        doNothing().when(shiftService).validateActive("s2"); doNothing().when(officeService).validateActive("o2");
        var created=service.create(employeeId,new CreateAssignmentRequest("s2","o2",LocalDate.of(2026,2,1),null,null),admin);
        assertThat(created.employeeId()).isEqualTo(employeeId);
        assertThat(service.resolve(employeeId,LocalDate.of(2026,1,15)).id()).isEqualTo(old.getId());
    }
}
