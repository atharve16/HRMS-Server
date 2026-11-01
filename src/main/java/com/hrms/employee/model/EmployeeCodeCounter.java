package com.hrms.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("employee_code_counters")
public class EmployeeCodeCounter {
    @Id
    private String id;
    private long sequence;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
}
