package com.hrms.payroll.dto;
import com.hrms.payroll.model.PayrollAdjustmentType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PayrollAdjustmentInput(@NotNull PayrollAdjustmentType type,@NotBlank @Size(max=200) String description,
                                     @NotNull @DecimalMin(value="0.01") BigDecimal amount) {}
