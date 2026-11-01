package com.hrms.employee.repository;

import com.hrms.employee.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
public class EmployeeRepositoryImpl implements EmployeeRepositoryCustom {
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("employeeCode", "employeeCode"), Map.entry("firstName", "firstName"),
            Map.entry("lastName", "lastName"), Map.entry("email", "email"),
            Map.entry("joiningDate", "joiningDate"), Map.entry("employmentStatus", "employmentStatus"),
            Map.entry("designation", "designation"), Map.entry("createdAt", "createdAt"));
    private final MongoTemplate mongoTemplate;

    public EmployeeRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Employee> search(EmployeeSearch search) {
        Criteria criteria = new Criteria();
        if (search.search() != null && !search.search().isBlank()) {
            String regex = ".*" + Pattern.quote(search.search().strip()) + ".*";
            criteria.orOperator(Criteria.where("employeeCode").regex(regex, "i"),
                    Criteria.where("firstName").regex(regex, "i"),
                    Criteria.where("lastName").regex(regex, "i"),
                    Criteria.where("email").regex(regex, "i"));
        }
        if (search.employmentStatus() != null) criteria.and("employmentStatus").is(search.employmentStatus());
        if (search.employmentType() != null) criteria.and("employmentType").is(search.employmentType());
        if (search.departmentId() != null && !search.departmentId().isBlank()) criteria.and("departmentId").is(search.departmentId());
        if (search.designation() != null && !search.designation().isBlank())
            criteria.and("designation").regex(Pattern.quote(search.designation().strip()), "i");
        String sortField = SORT_FIELDS.getOrDefault(search.sortBy(), "createdAt");
        Sort.Direction direction = "desc".equalsIgnoreCase(search.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(search.page(), search.limit(), Sort.by(direction, sortField));
        Query query = Query.query(criteria).with(pageable);
        Query countQuery = Query.query(criteria);
        return new PageImpl<>(mongoTemplate.find(query, Employee.class), pageable,
                mongoTemplate.count(countQuery, Employee.class));
    }
}
