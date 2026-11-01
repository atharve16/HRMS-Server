package com.hrms.leave.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.leave.dto.*;
import com.hrms.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees/{employeeId}/leave-balance")
@Tag(name = "Admin leave balances")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeLeaveBalanceController {
    private final LeaveService service;
    private final ClockSource clock;

    public AdminEmployeeLeaveBalanceController(LeaveService service, ClockSource clock) {
        this.service = service; this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<LeaveBalanceResponse>> get(@PathVariable String employeeId,
                                                        @RequestParam(required = false) Integer year) {
        return ApiResponse.success("Employee leave balances retrieved", service.employeeBalances(employeeId, year), clock);
    }

    @PutMapping
    public ApiResponse<LeaveBalanceResponse> allocate(@PathVariable String employeeId,
            @Valid @RequestBody AllocateLeaveBalanceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave balance allocated", service.allocate(employeeId, request, principal), clock);
    }
}
