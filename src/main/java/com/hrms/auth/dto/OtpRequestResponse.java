package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OtpRequestResponse(
        @Schema(example = "300") long expiresInSeconds
) {
}
