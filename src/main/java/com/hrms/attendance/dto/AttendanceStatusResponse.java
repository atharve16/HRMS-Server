package com.hrms.attendance.dto;
public record AttendanceStatusResponse(boolean activeSession, AttendanceResponse attendance, String lastCheckInAt) { }
