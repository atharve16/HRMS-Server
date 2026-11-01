package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthenticationResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresInSeconds,
        AuthenticatedUserResponse user
) {
}
