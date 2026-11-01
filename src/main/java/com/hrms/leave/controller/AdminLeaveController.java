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

@RestController
@RequestMapping("/api/v1/admin/leaves")
@Tag(name = "Admin leave")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLeaveController {
    private final LeaveService service;
    private final ClockSource clock;

    public AdminLeaveController(LeaveService service, ClockSource clock) { this.service = service; this.clock = clock; }

    @GetMapping
    public ApiResponse<LeaveRequestPageResponse> list(@RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String employeeCode, @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) LeaveStatus status, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return ApiResponse.success("Leave requests retrieved", service.adminList(new LeaveSearch(employeeId,
                employeeCode, leaveType, status, fromDate, toDate, page, limit, sortBy, sortDirection)), clock);
    }

    @GetMapping("/{leaveId}")
    public ApiResponse<LeaveRequestResponse> detail(@PathVariable String leaveId) {
        return ApiResponse.success("Leave request retrieved", service.detail(leaveId), clock);
    }

    @PatchMapping("/{leaveId}/approve")
    public ApiResponse<LeaveRequestResponse> approve(@PathVariable String leaveId,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave request approved", service.approve(leaveId, principal), clock);
    }

    @PatchMapping("/{leaveId}/reject")
    public ApiResponse<LeaveRequestResponse> reject(@PathVariable String leaveId,
            @Valid @RequestBody RejectLeaveRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Leave request rejected", service.reject(leaveId, request, principal), clock);
    }
}
