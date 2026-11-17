package com.hrms.leave.dto;

import java.math.BigDecimal;
import java.util.List;

public record LeaveDashboardResponse(String month, BigDecimal totalAllocated, BigDecimal totalConsumed,
                                     long pendingApplications, long lowBalanceEmployees,
                                     List<LeavePeriodBalanceResponse> balances) {}
