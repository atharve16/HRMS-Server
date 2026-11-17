package com.hrms.attendance;

import com.hrms.attendance.dto.AttendanceSearch;
import com.hrms.attendance.dto.AttendanceSummaryResponse;
import com.hrms.attendance.model.AttendanceRecord;
import com.hrms.attendance.model.AttendanceStatus;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.attendance.service.AttendanceCalculationService;
import com.hrms.attendance.service.AttendanceScheduleService;
import com.hrms.attendance.service.AttendanceService;
import com.hrms.attendance.service.GeofenceService;
import com.hrms.audit.service.AuditService;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.config.AttendanceProperties;
import com.hrms.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {
    @Test
    void returnsZeroSummaryForEmptyCollection() {
        AttendanceRepository records = mock(AttendanceRepository.class);
        when(records.search(any())).thenReturn(new PageImpl<>(List.of()));
        AttendanceService service = service(records);

        AttendanceSummaryResponse summary = service.summary(search(
                LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 14)));

        assertThat(summary).isEqualTo(new AttendanceSummaryResponse(0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void aggregatesStatusesAndHandlesRecordsWithNoSessions() {
        AttendanceRecord present = new AttendanceRecord();
        present.setAttendanceStatus(AttendanceStatus.PRESENT);
        present.setTotalWorkedMinutes(480);
        present.setLateByMinutes(5);
        present.setOvertimeMinutes(20);
        present.setSessions(null);
        AttendanceRecord partial = new AttendanceRecord();
        partial.setAttendanceStatus(AttendanceStatus.PARTIAL);
        partial.setTotalWorkedMinutes(120);
        partial.setEarlyDepartureByMinutes(30);
        AttendanceRecord incomplete = new AttendanceRecord();
        incomplete.setAttendanceStatus(AttendanceStatus.INCOMPLETE);

        AttendanceRepository records = mock(AttendanceRepository.class);
        when(records.search(any())).thenReturn(new PageImpl<>(List.of(present, partial, incomplete)));
        AttendanceSummaryResponse summary = service(records).summary(search(null, null));

        assertThat(summary.totalRecords()).isEqualTo(3);
        assertThat(summary.presentCount()).isEqualTo(1);
        assertThat(summary.partialCount()).isEqualTo(1);
        assertThat(summary.incompleteCount()).isEqualTo(1);
        assertThat(summary.totalWorkedMinutes()).isEqualTo(600);
        assertThat(summary.totalLateMinutes()).isEqualTo(5);
        assertThat(summary.totalEarlyDepartureMinutes()).isEqualTo(30);
        assertThat(summary.totalOvertimeMinutes()).isEqualTo(20);
        assertThat(present.getSessions()).isEmpty();
    }

    @Test
    void rejectsReversedDateRangeBeforeQueryingMongo() {
        AttendanceRepository records = mock(AttendanceRepository.class);
        AttendanceService service = service(records);

        assertThatThrownBy(() -> service.summary(search(
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 14))))
                .isInstanceOf(ApiException.class)
                .hasMessage("toDate cannot be earlier than fromDate");
        verifyNoInteractions(records);
    }

    private static AttendanceSearch search(LocalDate from, LocalDate to) {
        return new AttendanceSearch(null, null, null, null, null, null, from, to,
                0, 100, "workDate", "desc");
    }

    private static AttendanceService service(AttendanceRepository records) {
        return new AttendanceService(records, mock(EmployeeRepository.class),
                mock(AttendanceScheduleService.class), mock(GeofenceService.class),
                mock(AttendanceCalculationService.class), mock(CompanyTimeProvider.class),
                mock(AuditService.class), new AttendanceProperties(120, 240, 240, 24));
    }
}
