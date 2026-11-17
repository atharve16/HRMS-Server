package com.hrms.leave.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.leave.dto.LeavePeriodBalanceResponse;
import com.hrms.leave.service.LeaveAllocationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/leaves/me") @PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeLeaveAllocationController {
    private final LeaveAllocationService service; private final ClockSource clock;
    public EmployeeLeaveAllocationController(LeaveAllocationService service, ClockSource clock) { this.service = service; this.clock = clock; }
    @GetMapping("/period-balance") public ApiResponse<List<LeavePeriodBalanceResponse>> balances(@RequestParam String month, @AuthenticationPrincipal AuthPrincipal principal) { return ApiResponse.success("Monthly leave balances retrieved", service.employeeBalancesByPrincipal(principal, month), clock); }
}
