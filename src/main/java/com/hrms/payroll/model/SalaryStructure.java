package com.hrms.payroll.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.*;

@Document("salary_structures")
@CompoundIndexes({
        @CompoundIndex(name = "employee_effective_from_unique", def = "{'employeeId':1,'effectiveFrom':1}", unique = true),
        @CompoundIndex(name = "employee_active_salary_unique", def = "{'employeeId':1}", unique = true,
                partialFilter = "{'active':true}")
})
public class SalaryStructure {
    @Id private String id;
    private String employeeId;
    private BigDecimal monthlyFixedSalary;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private String createdBy;
    private String updatedBy;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public BigDecimal getMonthlyFixedSalary(){return monthlyFixedSalary;} public void setMonthlyFixedSalary(BigDecimal v){monthlyFixedSalary=v;}
    public LocalDate getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(LocalDate v){effectiveFrom=v;}
    public LocalDate getEffectiveTo(){return effectiveTo;} public void setEffectiveTo(LocalDate v){effectiveTo=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
