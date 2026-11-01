package com.hrms.employee.service;

import com.hrms.config.EmployeeProperties;
import com.hrms.employee.model.EmployeeCodeCounter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class EmployeeCodeGenerator {
    private static final String COUNTER_ID = "employee";
    private final MongoTemplate mongoTemplate;
    private final EmployeeProperties properties;

    public EmployeeCodeGenerator(MongoTemplate mongoTemplate, EmployeeProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    public String next() {
        EmployeeCodeCounter counter = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(COUNTER_ID)),
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                EmployeeCodeCounter.class);
        if (counter == null) {
            throw new IllegalStateException("Employee code counter could not be allocated");
        }
        return "%s-%04d".formatted(properties.codePrefix(), counter.getSequence());
    }
}
