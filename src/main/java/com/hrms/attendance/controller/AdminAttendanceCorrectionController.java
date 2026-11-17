package com.hrms.attendance.controller;

import com.hrms.attendance.dto.*;
import com.hrms.attendance.model.AttendanceCorrectionStatus;
import com.hrms.attendance.service.AttendanceCorrectionService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/admin/attendance/correction-requests") @PreAuthorize("hasRole('ADMIN')")
public class AdminAttendanceCorrectionController {
    private final AttendanceCorrectionService service; private final ClockSource clock;
    public AdminAttendanceCorrectionController(AttendanceCorrectionService service, ClockSource clock) { this.service=service; this.clock=clock; }
    @GetMapping public ApiResponse<org.springframework.data.domain.Page<AttendanceCorrectionResponse>> list(@RequestParam(required=false) String employeeId, @RequestParam(required=false) String departmentId, @RequestParam(required=false) LocalDate fromDate, @RequestParam(required=false) LocalDate toDate, @RequestParam(required=false) AttendanceCorrectionStatus status, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int limit) { return ApiResponse.success("Attendance correction requests retrieved", service.adminList(employeeId, departmentId, fromDate, toDate, status, page, limit), clock); }
    @GetMapping("/{requestId}") public ApiResponse<AttendanceCorrectionResponse> detail(@PathVariable String requestId) { return ApiResponse.success("Attendance correction request retrieved", service.adminDetail(requestId), clock); }
    @PostMapping("/{requestId}/approve") public ApiResponse<AttendanceCorrectionResponse> approve(@PathVariable String requestId, @Valid @RequestBody(required=false) ReviewAttendanceCorrectionRequest request, @AuthenticationPrincipal AuthPrincipal admin) { return ApiResponse.success("Attendance correction request approved", service.approve(requestId, request, admin), clock); }
    @PostMapping("/{requestId}/reject") public ApiResponse<AttendanceCorrectionResponse> reject(@PathVariable String requestId, @Valid @RequestBody(required=false) ReviewAttendanceCorrectionRequest request, @AuthenticationPrincipal AuthPrincipal admin) { return ApiResponse.success("Attendance correction request rejected", service.reject(requestId, request, admin), clock); }
}
