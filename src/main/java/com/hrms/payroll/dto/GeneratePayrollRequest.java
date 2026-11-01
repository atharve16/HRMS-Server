package com.hrms.payroll.dto;
import jakarta.validation.constraints.*;
public record GeneratePayrollRequest(@Min(1) @Max(12) int month,@Min(2000) @Max(2200) int year) {}
