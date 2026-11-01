package com.hrms.employee.controller;

import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import com.hrms.employee.dto.CreateEmployeeRequest;
import com.hrms.employee.dto.EmployeePageResponse;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.UpdateEmployeeRequest;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.model.EmploymentType;
import com.hrms.employee.repository.EmployeeSearch;
import com.hrms.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Employees", description = "Employee administration and self-profile")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final ClockSource clockSource;

    public EmployeeController(EmployeeService employeeService, ClockSource clockSource) {
        this.employeeService = employeeService;
        this.clockSource = clockSource;
    }

    @PostMapping("/admin/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an employee account")
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request,
                                                 @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.success("Employee created", employeeService.create(request, actor), clockSource);
    }

    @GetMapping("/admin/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List employees with filtering and pagination")
    public ApiResponse<EmployeePageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmploymentStatus employmentStatus,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String designation,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        EmployeeSearch filter = new EmployeeSearch(search, employmentStatus, employmentType, departmentId,
                designation, page, limit, sortBy, sortDirection);
        return ApiResponse.success("Employees retrieved", employeeService.list(filter), clockSource);
    }

    @GetMapping("/admin/employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get employee details")
    public ApiResponse<EmployeeResponse> get(@PathVariable String employeeId) {
        return ApiResponse.success("Employee retrieved", employeeService.get(employeeId), clockSource);
    }

    @PatchMapping("/admin/employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Partially update an employee")
    public ApiResponse<EmployeeResponse> update(@PathVariable String employeeId,
                                                @Valid @RequestBody UpdateEmployeeRequest request,
                                                @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.success("Employee updated", employeeService.update(employeeId, request, actor), clockSource);
    }

    @PatchMapping("/admin/employees/{employeeId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an employee account")
    public ApiResponse<EmployeeResponse> deactivate(@PathVariable String employeeId,
                                                    @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.success("Employee deactivated", employeeService.deactivate(employeeId, actor), clockSource);
    }

    @PatchMapping("/admin/employees/{employeeId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate an employee account")
    public ApiResponse<EmployeeResponse> reactivate(@PathVariable String employeeId,
                                                    @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.success("Employee reactivated", employeeService.reactivate(employeeId, actor), clockSource);
    }

    @GetMapping("/employees/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get the authenticated employee's profile")
    public ApiResponse<EmployeeResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Employee profile retrieved", employeeService.current(principal), clockSource);
    }
}
