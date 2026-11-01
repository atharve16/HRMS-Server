package com.hrms.leave.repository;

import com.hrms.leave.dto.LeaveSearch;
import com.hrms.leave.model.LeaveRequest;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class LeaveRequestRepositoryImpl implements LeaveRequestRepositoryCustom {
    private static final Map<String, String> SORTS = Map.of(
            "createdAt", "createdAt", "startDate", "startDate", "endDate", "endDate",
            "status", "status", "leaveType", "leaveType", "employeeCode", "employeeCodeSnapshot");
    private final MongoTemplate template;

    public LeaveRequestRepositoryImpl(MongoTemplate template) { this.template = template; }

    @Override
    public Page<LeaveRequest> search(LeaveSearch s) {
        Criteria criteria = new Criteria();
        if (hasText(s.employeeId())) criteria.and("employeeId").is(s.employeeId().strip());
        if (hasText(s.employeeCode())) criteria.and("employeeCodeSnapshot").is(s.employeeCode().strip());
        if (s.leaveType() != null) criteria.and("leaveType").is(s.leaveType());
        if (s.status() != null) criteria.and("status").is(s.status());
        if (s.fromDate() != null) criteria.and("endDate").gte(s.fromDate());
        if (s.toDate() != null) criteria.and("startDate").lte(s.toDate());
        Sort.Direction direction = "asc".equalsIgnoreCase(s.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(s.page(), s.limit(),
                Sort.by(direction, SORTS.getOrDefault(s.sortBy(), "createdAt")));
        Query query = Query.query(criteria).with(pageable);
        return new PageImpl<>(template.find(query, LeaveRequest.class), pageable,
                template.count(Query.query(criteria), LeaveRequest.class));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
