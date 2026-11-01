package com.hrms.holiday.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;

@Document("company_holidays")
@CompoundIndexes({
        @CompoundIndex(name = "active_holiday_date_unique", def = "{'date':1}", unique = true,
                partialFilter = "{'active':true}"),
        @CompoundIndex(name = "holiday_date_active_idx", def = "{'date':1,'active':1}")
})
public class CompanyHoliday {
    @Id private String id;
    private String name;
    private String description;
    private LocalDate date;
    private boolean active;
    private String createdBy;
    private String updatedBy;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public LocalDate getDate() { return date; } public void setDate(LocalDate v) { date = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String v) { updatedBy = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
