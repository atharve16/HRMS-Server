package com.hrms.shift.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

@Document("shifts")
public class Shift {
    @Id private String id;
    @Indexed(unique = true) private String shiftCode;
    private String name;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private Set<DayOfWeek> workingDays = EnumSet.noneOf(DayOfWeek.class);
    private int gracePeriodMinutes;
    private int earlyDepartureGraceMinutes;
    private int expectedBreakMinutes;
    private Integer minimumBreakMinutes;
    private boolean flexible;
    @Indexed private ShiftStatus status;
    private String createdBy;
    private String updatedBy;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId() { return id; } public void setId(String v) { id=v; }
    public String getShiftCode() { return shiftCode; } public void setShiftCode(String v) { shiftCode=v; }
    public String getName() { return name; } public void setName(String v) { name=v; }
    public String getDescription() { return description; } public void setDescription(String v) { description=v; }
    public LocalTime getStartTime() { return startTime; } public void setStartTime(LocalTime v) { startTime=v; }
    public LocalTime getEndTime() { return endTime; } public void setEndTime(LocalTime v) { endTime=v; }
    public Set<DayOfWeek> getWorkingDays() { return workingDays; } public void setWorkingDays(Set<DayOfWeek> v) { workingDays=v; }
    public int getGracePeriodMinutes() { return gracePeriodMinutes; } public void setGracePeriodMinutes(int v) { gracePeriodMinutes=v; }
    public int getEarlyDepartureGraceMinutes() { return earlyDepartureGraceMinutes; } public void setEarlyDepartureGraceMinutes(int v) { earlyDepartureGraceMinutes=v; }
    public int getExpectedBreakMinutes() { return expectedBreakMinutes; } public void setExpectedBreakMinutes(int v) { expectedBreakMinutes=v; }
    public Integer getMinimumBreakMinutes() { return minimumBreakMinutes; } public void setMinimumBreakMinutes(Integer v) { minimumBreakMinutes=v; }
    public boolean isFlexible() { return flexible; } public void setFlexible(boolean v) { flexible=v; }
    public ShiftStatus getStatus() { return status; } public void setStatus(ShiftStatus v) { status=v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy=v; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String v) { updatedBy=v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt=v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt=v; }
}
