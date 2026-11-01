package com.hrms.attendance.dto;
public record AttendanceSummaryResponse(long totalRecords,long presentCount,long partialCount,long incompleteCount,long totalWorkedMinutes,long totalLateMinutes,long totalEarlyDepartureMinutes,long totalOvertimeMinutes) { }
