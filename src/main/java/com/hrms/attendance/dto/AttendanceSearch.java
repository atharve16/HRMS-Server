package com.hrms.attendance.dto;
import com.hrms.attendance.model.AttendanceStatus;import java.time.LocalDate;
public record AttendanceSearch(String employeeId,String employeeCode,String departmentId,String officeLocationId,String shiftId,AttendanceStatus status,LocalDate fromDate,LocalDate toDate,int page,int limit,String sortBy,String sortDirection) { }
