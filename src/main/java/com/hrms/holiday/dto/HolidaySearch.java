package com.hrms.holiday.dto;

import java.time.LocalDate;

public record HolidaySearch(Integer year, LocalDate fromDate, LocalDate toDate, Boolean active,
                            int page, int limit, String sortDirection) {}
