package com.hrms.attendance;

import com.hrms.attendance.dto.AttendanceSearch;
import com.hrms.attendance.model.AttendanceRecord;
import com.hrms.attendance.repository.AttendanceRepositoryImpl;
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

class AttendanceRepositoryTest {
    @Test
    void buildsOneInclusiveWorkDateCriterionForSameDaySummary() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.find(any(Query.class), eq(AttendanceRecord.class))).thenReturn(List.of());
        when(template.count(any(Query.class), eq(AttendanceRecord.class))).thenReturn(0L);

        AttendanceRepositoryImpl repository = new AttendanceRepositoryImpl(template);
        LocalDate date = LocalDate.of(2026, 11, 14);
        Page<AttendanceRecord> result = repository.search(new AttendanceSearch(
                null, null, null, null, null, null, date, date,
                0, 100, "workDate", "asc"));

        assertThat(result.getTotalElements()).isZero();
        ArgumentCaptor<Query> queries = ArgumentCaptor.forClass(Query.class);
        verify(template).find(queries.capture(), eq(AttendanceRecord.class));
        Document workDate = (Document) queries.getValue().getQueryObject().get("workDate");
        assertThat(workDate).containsKeys("$gte", "$lte");
        assertThat(workDate.get("$gte")).isEqualTo(date);
        assertThat(workDate.get("$lte")).isEqualTo(date);
    }
}
