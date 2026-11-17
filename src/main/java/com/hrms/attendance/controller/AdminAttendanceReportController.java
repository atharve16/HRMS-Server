package com.hrms.attendance.controller;

import com.hrms.attendance.dto.AttendanceMonthlyReportResponse;
import com.hrms.attendance.service.AttendanceReportService;
import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/attendance/reports")
@Tag(name = "Admin attendance reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAttendanceReportController {
    private final AttendanceReportService reports;
    private final ClockSource clock;

    public AdminAttendanceReportController(AttendanceReportService reports, ClockSource clock) {
        this.reports = reports;
        this.clock = clock;
    }

    @GetMapping("/monthly")
    public ApiResponse<AttendanceMonthlyReportResponse> monthly(
            @RequestParam String month,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String employeeCode
    ) {
        return ApiResponse.success(
                "Monthly attendance report retrieved",
                reports.monthly(month, departmentId, employeeCode),
                clock
        );
    }
}
