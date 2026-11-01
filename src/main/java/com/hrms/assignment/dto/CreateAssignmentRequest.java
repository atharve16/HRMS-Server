package com.hrms.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateAssignmentRequest(@NotBlank String shiftId, @NotBlank String officeLocationId,
                                      @NotNull LocalDate effectiveFrom, LocalDate effectiveTo,
                                      @Size(max = 500) String reason) { }
