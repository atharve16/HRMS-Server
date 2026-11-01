package com.hrms.holiday.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.holiday.dto.EmployeeHolidayResponse;
import com.hrms.holiday.service.HolidayService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@Tag(name = "Employee holidays")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class HolidayController {
    private final HolidayService service; private final ClockSource clock;
    public HolidayController(HolidayService service, ClockSource clock) { this.service = service; this.clock = clock; }

    @GetMapping
    public ApiResponse<List<EmployeeHolidayResponse>> list(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Company holidays retrieved",
                service.employeeList(year, fromDate, toDate, principal), clock);
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<EmployeeHolidayResponse>> upcoming(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Upcoming company holidays retrieved", service.upcoming(principal), clock);
    }
}
