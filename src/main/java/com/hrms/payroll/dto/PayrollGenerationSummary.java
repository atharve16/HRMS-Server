package com.hrms.payroll.dto;
import java.util.List;
public record PayrollGenerationSummary(int month,int year,List<PayrollGenerationItem> generated,
                                       List<PayrollGenerationItem> skipped,List<PayrollGenerationItem> failed) {}
