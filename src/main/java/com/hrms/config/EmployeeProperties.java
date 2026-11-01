package com.hrms.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.employee")
public record EmployeeProperties(
        @NotBlank @Pattern(regexp = "[A-Z0-9]{2,10}") String codePrefix
) {
}
