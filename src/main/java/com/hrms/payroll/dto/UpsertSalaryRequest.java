package com.hrms.payroll.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record UpsertSalaryRequest(@NotNull @DecimalMin(value="0.01") BigDecimal monthlyFixedSalary,
                                  @NotNull LocalDate effectiveFrom) {}
