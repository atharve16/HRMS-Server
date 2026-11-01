package com.hrms.payroll.controller;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.service.SalaryStructureService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/admin/employees/{employeeId}/salary") @PreAuthorize("hasRole('ADMIN')")
@Tag(name="Admin salary") @SecurityRequirement(name="bearerAuth")
public class AdminSalaryController {
 private final SalaryStructureService service;private final ClockSource clock;
 public AdminSalaryController(SalaryStructureService service,ClockSource clock){this.service=service;this.clock=clock;}
 @PutMapping public ApiResponse<SalaryStructureResponse> upsert(@PathVariable String employeeId,@Valid @RequestBody UpsertSalaryRequest request,
        @AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Salary structure saved",service.upsert(employeeId,request,principal),clock);}
 @GetMapping public ApiResponse<SalaryStructureResponse> current(@PathVariable String employeeId){return ApiResponse.success("Salary structure retrieved",service.current(employeeId),clock);}
 @GetMapping("/history") public ApiResponse<List<SalaryStructureResponse>> history(@PathVariable String employeeId){return ApiResponse.success("Salary history retrieved",service.history(employeeId),clock);}
}
