package com.hrms.leave.dto;

import com.hrms.leave.model.*;
import java.time.LocalDate;

public record LeaveSearch(String employeeId, String employeeCode, LeaveType leaveType, LeaveStatus status,
                          LocalDate fromDate, LocalDate toDate, int page, int limit,
                          String sortBy, String sortDirection) {}
