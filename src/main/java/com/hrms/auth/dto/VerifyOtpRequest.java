package com.hrms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank @Email @Size(max = 254)
        @Schema(example = "employee@example.com") String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must contain exactly six digits")
        @Schema(example = "123456") String otp
) {
}
