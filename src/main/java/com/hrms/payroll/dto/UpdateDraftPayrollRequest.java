package com.hrms.payroll.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
public record UpdateDraftPayrollRequest(@Valid @Size(max=50) List<PayrollAdjustmentInput> earnings,
                                        @Valid @Size(max=50) List<PayrollAdjustmentInput> deductions) {}
