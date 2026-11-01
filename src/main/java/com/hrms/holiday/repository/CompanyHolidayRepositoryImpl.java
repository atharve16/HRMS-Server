package com.hrms.holiday.repository;

import com.hrms.holiday.dto.HolidaySearch;
import com.hrms.holiday.model.CompanyHoliday;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyHolidayRepositoryImpl implements CompanyHolidayRepositoryCustom {
    private final MongoTemplate template;
    public CompanyHolidayRepositoryImpl(MongoTemplate template) { this.template = template; }

    @Override
    public Page<CompanyHoliday> search(HolidaySearch s) {
        Criteria criteria = new Criteria();
        if (s.fromDate() != null && s.toDate() != null) criteria.and("date").gte(s.fromDate()).lte(s.toDate());
        else if (s.fromDate() != null) criteria.and("date").gte(s.fromDate());
        else if (s.toDate() != null) criteria.and("date").lte(s.toDate());
        if (s.active() != null) criteria.and("active").is(s.active());
        Sort.Direction direction = "desc".equalsIgnoreCase(s.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(s.page(), s.limit(), Sort.by(direction, "date"));
        Query query = Query.query(criteria).with(pageable);
        return new PageImpl<>(template.find(query, CompanyHoliday.class), pageable,
                template.count(Query.query(criteria), CompanyHoliday.class));
    }
}
