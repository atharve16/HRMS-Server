package com.hrms.attendance.controller;

import com.hrms.attendance.dto.*;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.attendance.service.AttendanceCorrectionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/attendance/correction-requests") @PreAuthorize("hasRole('EMPLOYEE')")
public class AttendanceCorrectionController {
    private final AttendanceCorrectionService service; private final ClockSource clock;
    public AttendanceCorrectionController(AttendanceCorrectionService service, ClockSource clock) { this.service=service; this.clock=clock; }
    @PostMapping public ApiResponse<AttendanceCorrectionResponse> submit(@Valid @RequestBody SubmitAttendanceCorrectionRequest request, @AuthenticationPrincipal AuthPrincipal principal) { return ApiResponse.success("Attendance correction request submitted", service.submit(request, principal), clock); }
    @GetMapping("/my") public ApiResponse<List<AttendanceCorrectionResponse>> mine(@AuthenticationPrincipal AuthPrincipal principal) { return ApiResponse.success("Attendance correction requests retrieved", service.mine(principal), clock); }
    @GetMapping("/my/{requestId}") public ApiResponse<AttendanceCorrectionResponse> detail(@PathVariable String requestId, @AuthenticationPrincipal AuthPrincipal principal) { return ApiResponse.success("Attendance correction request retrieved", service.mineDetail(requestId, principal), clock); }
    @PostMapping("/{requestId}/cancel") public ApiResponse<AttendanceCorrectionResponse> cancel(@PathVariable String requestId, @AuthenticationPrincipal AuthPrincipal principal) { return ApiResponse.success("Attendance correction request cancelled", service.cancel(requestId, principal), clock); }
}
