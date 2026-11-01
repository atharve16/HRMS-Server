package com.hrms.payroll.controller;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/payroll/me") @PreAuthorize("hasRole('EMPLOYEE')")
@Tag(name="Employee payroll") @SecurityRequirement(name="bearerAuth")
public class EmployeePayrollController {
 private final PayrollService service;private final ClockSource clock;
 public EmployeePayrollController(PayrollService service,ClockSource clock){this.service=service;this.clock=clock;}
 @GetMapping public ApiResponse<EmployeePayrollPageResponse> mine(@RequestParam(required=false)Integer year,@RequestParam(defaultValue="0")int page,
  @RequestParam(defaultValue="20")int limit,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll records retrieved",service.mine(principal,year,page,limit),clock);}
 @GetMapping("/{payrollId}") public ApiResponse<EmployeePayrollResponse> detail(@PathVariable String payrollId,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll record retrieved",service.mineDetail(payrollId,principal),clock);}
}
