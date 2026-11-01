package com.hrms.office.repository;

import com.hrms.office.model.OfficeStatus;
public record OfficeSearch(String search, OfficeStatus status, int page, int limit, String sortBy, String sortDirection) { }
