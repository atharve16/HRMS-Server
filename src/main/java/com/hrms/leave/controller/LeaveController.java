package com.hrms.leave.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.leave.dto.*;
import com.hrms.leave.model.*;
import com.hrms.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@Tag(name = "Employee leave")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class LeaveController {
    private final LeaveService service;
    private final ClockSource clock;

    public LeaveController(LeaveService service, ClockSource clock) { this.service = service; this.clock = clock; }

    @PostMapping
    public ApiResponse<LeaveRequestResponse> apply(@Valid @RequestBody ApplyLeaveRequest request,
                                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave request submitted", service.apply(request, principal), clock);
    }

    @GetMapping("/me")
    public ApiResponse<LeaveRequestPageResponse> mine(@RequestParam(required = false) LeaveStatus status,
                                                       @RequestParam(required = false) LeaveType leaveType,
                                                       @RequestParam(required = false) LocalDate fromDate,
                                                       @RequestParam(required = false) LocalDate toDate,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int limit,
                                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave requests retrieved",
                service.mine(principal, status, leaveType, fromDate, toDate, page, limit), clock);
    }

    @GetMapping("/me/balance")
    public ApiResponse<List<LeaveBalanceResponse>> balances(@RequestParam(required = false) Integer year,
                                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave balances retrieved", service.myBalances(principal, year), clock);
    }

    @PatchMapping("/{leaveId}/cancel")
    public ApiResponse<LeaveRequestResponse> cancel(@PathVariable String leaveId,
                                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave request cancelled", service.cancel(leaveId, principal), clock);
    }
}
