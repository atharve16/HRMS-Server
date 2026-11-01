package com.hrms.holiday.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.holiday.dto.*;
import com.hrms.holiday.service.HolidayService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/holidays")
@Tag(name = "Admin holidays")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHolidayController {
    private final HolidayService service; private final ClockSource clock;
    public AdminHolidayController(HolidayService service, ClockSource clock) { this.service = service; this.clock = clock; }

    @PostMapping
    public ApiResponse<AdminHolidayResponse> create(@Valid @RequestBody CreateHolidayRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Company holiday created", service.create(request, principal), clock);
    }

    @GetMapping
    public ApiResponse<HolidayPageResponse> list(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "asc") String sortDirection) {
        return ApiResponse.success("Company holidays retrieved", service.list(new HolidaySearch(year, fromDate,
                toDate, active, page, limit, sortDirection)), clock);
    }

    @GetMapping("/{holidayId}")
    public ApiResponse<AdminHolidayResponse> detail(@PathVariable String holidayId) {
        return ApiResponse.success("Company holiday retrieved", service.detail(holidayId), clock);
    }

    @PatchMapping("/{holidayId}")
    public ApiResponse<AdminHolidayResponse> update(@PathVariable String holidayId,
            @Valid @RequestBody UpdateHolidayRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Company holiday updated", service.update(holidayId, request, principal), clock);
    }

    @PatchMapping("/{holidayId}/deactivate")
    public ApiResponse<AdminHolidayResponse> deactivate(@PathVariable String holidayId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Company holiday deactivated", service.deactivate(holidayId, principal), clock);
    }
}
