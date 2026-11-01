package com.hrms.common.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard API response envelope")
public record ApiResponse<T>(
        @Schema(example = "true") boolean success,
        @Schema(example = "Operation successful") String message,
        T data,
        @Schema(type = "string", format = "date-time") Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data, ClockSource clockSource) {
        return new ApiResponse<>(true, message, data, clockSource.now());
    }

    public static <T> ApiResponse<T> failure(String message, T data, ClockSource clockSource) {
        return new ApiResponse<>(false, message, data, clockSource.now());
    }
}
