package com.hrms.holiday.repository;

import com.hrms.holiday.model.CompanyHoliday;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.*;

public interface CompanyHolidayRepository extends MongoRepository<CompanyHoliday, String>, CompanyHolidayRepositoryCustom {
    boolean existsByDateAndActiveTrue(LocalDate date);
    boolean existsByDateAndActiveTrueAndIdNot(LocalDate date, String id);
    List<CompanyHoliday> findByDateBetweenAndActiveTrueOrderByDateAsc(LocalDate from, LocalDate to);
    List<CompanyHoliday> findByActiveTrueAndDateGreaterThanOrderByDateAsc(LocalDate date);
}
