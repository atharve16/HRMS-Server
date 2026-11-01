package com.hrms.shift.repository;
import com.hrms.shift.model.ShiftStatus;
public record ShiftSearch(String search, ShiftStatus status, int page, int limit, String sortBy, String sortDirection) { }
