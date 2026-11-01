package com.hrms.payroll.controller;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.*;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.model.PayrollStatus;
import com.hrms.payroll.repository.PayrollSearch;
import com.hrms.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/payroll") @PreAuthorize("hasRole('ADMIN')")
@Tag(name="Admin payroll") @SecurityRequirement(name="bearerAuth")
public class AdminPayrollController {
 private final PayrollService service;private final ClockSource clock;
 public AdminPayrollController(PayrollService service,ClockSource clock){this.service=service;this.clock=clock;}
 @PostMapping("/generate") public ApiResponse<PayrollGenerationSummary> generate(@Valid @RequestBody GeneratePayrollRequest request,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll generation completed",service.generate(request,principal),clock);}
 @GetMapping public ApiResponse<PayrollPageResponse> list(@RequestParam(required=false)Integer month,@RequestParam(required=false)Integer year,
  @RequestParam(required=false)String employeeId,@RequestParam(required=false)String employeeCode,@RequestParam(required=false)String departmentId,
  @RequestParam(required=false)PayrollStatus status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int limit,
  @RequestParam(defaultValue="desc")String sortDirection){return ApiResponse.success("Payroll records retrieved",service.list(new PayrollSearch(month,year,employeeId,employeeCode,departmentId,status,page,limit,sortDirection)),clock);}
 @GetMapping("/{payrollId}") public ApiResponse<PayrollResponse> detail(@PathVariable String payrollId){return ApiResponse.success("Payroll record retrieved",service.detail(payrollId),clock);}
 @PatchMapping("/{payrollId}") public ApiResponse<PayrollResponse> update(@PathVariable String payrollId,@Valid @RequestBody UpdateDraftPayrollRequest request,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Draft payroll updated",service.updateDraft(payrollId,request,principal),clock);}
 @PatchMapping("/{payrollId}/review") public ApiResponse<PayrollResponse> review(@PathVariable String payrollId,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll reviewed",service.review(payrollId,principal),clock);}
 @PatchMapping("/{payrollId}/finalize") public ApiResponse<PayrollResponse> finalizePayroll(@PathVariable String payrollId,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll finalized",service.finalizePayroll(payrollId,principal),clock);}
 @PatchMapping("/{payrollId}/pay") public ApiResponse<PayrollResponse> pay(@PathVariable String payrollId,@AuthenticationPrincipal AuthPrincipal principal){return ApiResponse.success("Payroll marked as paid",service.pay(payrollId,principal),clock);}
}
