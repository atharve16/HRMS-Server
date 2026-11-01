package com.hrms.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EndAssignmentRequest(@NotNull LocalDate effectiveTo, @Size(max = 500) String reason) { }
