package com.hrms.leave.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.leave.dto.*;
import com.hrms.leave.service.LeaveAllocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/admin/leave-allocation") @PreAuthorize("hasRole('ADMIN')")
public class AdminLeaveAllocationController {
    private final LeaveAllocationService service; private final ClockSource clock;
    public AdminLeaveAllocationController(LeaveAllocationService service, ClockSource clock) { this.service = service; this.clock = clock; }
    @GetMapping("/employees/{employeeId}/balances") public ApiResponse<List<LeavePeriodBalanceResponse>> balances(@PathVariable String employeeId, @RequestParam String month) { return ApiResponse.success("Leave period balances retrieved", service.employeeBalances(employeeId, month), clock); }
    @GetMapping("/employees/{employeeId}/history") public ApiResponse<List<LeaveLedgerEntryResponse>> history(@PathVariable String employeeId, @RequestParam String month) { return ApiResponse.success("Leave allocation history retrieved", service.history(employeeId, month), clock); }
    @PostMapping("/employees/{employeeId}/allocations") public ApiResponse<LeavePeriodBalanceResponse> allocate(@PathVariable String employeeId, @Valid @RequestBody LeaveAllocationRequest request, @AuthenticationPrincipal AuthPrincipal admin) { return ApiResponse.success("Leave allocation recorded", service.allocate(employeeId, request, admin), clock); }
    @PutMapping("/policies/company") public ApiResponse<LeavePolicyResponse> companyPolicy(@Valid @RequestBody LeavePolicyRequest request, @AuthenticationPrincipal AuthPrincipal admin) { return ApiResponse.success("Company leave policy saved", service.saveCompanyPolicy(request, admin), clock); }
    @PutMapping("/employees/{employeeId}/policies") public ApiResponse<LeavePolicyResponse> employeePolicy(@PathVariable String employeeId, @Valid @RequestBody LeavePolicyRequest request, @AuthenticationPrincipal AuthPrincipal admin) { return ApiResponse.success("Employee leave policy saved", service.saveEmployeePolicy(employeeId, request, admin), clock); }
    @GetMapping("/policies") public ApiResponse<List<LeavePolicyResponse>> policies(@RequestParam(required = false) String employeeId) { return ApiResponse.success("Leave policies retrieved", service.policies(employeeId), clock); }
    @GetMapping("/dashboard") public ApiResponse<LeaveDashboardResponse> dashboard(@RequestParam String month) { return ApiResponse.success("Leave dashboard retrieved", service.dashboard(month), clock); }
}
