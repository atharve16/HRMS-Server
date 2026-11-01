package com.hrms.common.controller;

import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import com.hrms.common.utils.CompanyTimeProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Public service readiness information")
public class SystemController {

    private final ClockSource clockSource;
    private final CompanyTimeProvider companyTimeProvider;

    public SystemController(ClockSource clockSource, CompanyTimeProvider companyTimeProvider) {
        this.clockSource = clockSource;
        this.companyTimeProvider = companyTimeProvider;
    }

    @GetMapping("/health")
    @Operation(summary = "Check whether the HTTP API is available", security = {})
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success("Service is available", Map.of(
                "status", "UP",
                "companyTimeZone", companyTimeProvider.companyZoneId().getId()
        ), clockSource);
    }
}
