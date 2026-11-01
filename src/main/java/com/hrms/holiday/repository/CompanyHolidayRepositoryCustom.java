package com.hrms.holiday.repository;

import com.hrms.holiday.dto.HolidaySearch;
import com.hrms.holiday.model.CompanyHoliday;
import org.springframework.data.domain.Page;

public interface CompanyHolidayRepositoryCustom {
    Page<CompanyHoliday> search(HolidaySearch search);
}
