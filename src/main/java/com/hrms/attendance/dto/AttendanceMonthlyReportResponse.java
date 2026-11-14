package com.hrms.attendance.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AttendanceMonthlyReportResponse(
        String month,
        List<LocalDate> dates,
        List<EmployeeRow> employees,
        Summary summary
) {
    public record EmployeeRow(
            String employeeId,
            String employeeCode,
            String employeeName,
            String departmentId,
            Map<String, DayEntry> days,
            long presentDays,
            long halfDays,
            long absentDays,
            long totalWorkedMinutes,
            long totalOvertimeMinutes,
            long totalLateMinutes
    ) {}

    public record DayEntry(
            String status,
            long workedMinutes,
            long overtimeMinutes,
            long lateByMinutes,
            long scheduledMinutes
    ) {}

    public record Summary(
            long employeeCount,
            long presentDays,
            long halfDays,
            long absentDays,
            long totalWorkedMinutes,
            long totalOvertimeMinutes,
            long totalLateMinutes
    ) {}
}
