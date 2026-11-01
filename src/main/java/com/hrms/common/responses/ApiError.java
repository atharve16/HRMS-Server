package com.hrms.common.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Machine-readable error details")
public record ApiError(
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(description = "Request path that failed") String path,
        @Schema(description = "Field-specific or contextual details") Map<String, String> details
) {
}
