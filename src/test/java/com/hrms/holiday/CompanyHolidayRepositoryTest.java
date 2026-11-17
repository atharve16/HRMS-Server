package com.hrms.holiday;

import com.hrms.holiday.dto.HolidaySearch;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepositoryImpl;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CompanyHolidayRepositoryTest {
    @Test
    void supportsAuthenticatedAdminFilterShapeAndEmptyResults() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.find(any(Query.class), eq(CompanyHoliday.class))).thenReturn(List.of());
        when(template.count(any(Query.class), eq(CompanyHoliday.class))).thenReturn(0L);

        LocalDate from = LocalDate.of(2026, 11, 14);
        Page<CompanyHoliday> page = new CompanyHolidayRepositoryImpl(template).search(
                new HolidaySearch(null, from, null, true, 0, 5, "asc"));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(template).find(queryCaptor.capture(), eq(CompanyHoliday.class));
        Document query = queryCaptor.getValue().getQueryObject();
        assertThat((Document) query.get("date")).containsEntry("$gte", from);
        assertThat(query).containsEntry("active", true);
    }
}
