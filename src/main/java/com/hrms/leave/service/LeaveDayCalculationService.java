package com.hrms.leave.service;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.common.exceptions.*;
import com.hrms.shift.model.Shift;
import com.hrms.shift.repository.ShiftRepository;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class LeaveDayCalculationService {
    private final AssignmentService assignments;
    private final ShiftRepository shifts;
    private final CompanyHolidayRepository holidays;

    public LeaveDayCalculationService(AssignmentService assignments, ShiftRepository shifts,
                                      CompanyHolidayRepository holidays) {
        this.assignments = assignments;
        this.shifts = shifts;
        this.holidays = holidays;
    }

    public List<LocalDate> calculate(String employeeId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Leave end date cannot be earlier than start date");
        }
        Set<LocalDate> activeHolidays = holidays
                .findByDateBetweenAndActiveTrueOrderByDateAsc(startDate, endDate).stream()
                .map(com.hrms.holiday.model.CompanyHoliday::getDate)
                .collect(java.util.stream.Collectors.toSet());
        List<LocalDate> leaveDays = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            AssignmentResponse assignment;
            try {
                assignment = assignments.resolve(employeeId, date);
            } catch (ApiException exception) {
                throw new ApiException(ErrorCode.NO_WORK_ASSIGNMENT, HttpStatus.CONFLICT,
                        "No valid work assignment exists for " + date);
            }
            Shift shift = shifts.findById(assignment.shiftId()).orElseThrow(() ->
                    new ApiException(ErrorCode.NO_WORK_ASSIGNMENT, HttpStatus.CONFLICT,
                            "The assigned shift was not found for " + currentDate));
            if (shift.getWorkingDays().contains(date.getDayOfWeek()) && !activeHolidays.contains(date)) {
                leaveDays.add(date);
            }
        }
        if (leaveDays.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "The requested range contains no scheduled working days after weekly off and holiday exclusions");
        }
        return List.copyOf(leaveDays);
    }
}
