package com.hrms.attendance.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.*;

@Document("attendance_correction_requests")
@CompoundIndex(name = "employee_date_pending_unique", def = "{'employeeId':1,'attendanceDate':1,'status':1}", unique = true,
        partialFilter = "{'status':'PENDING'}")
public class AttendanceCorrectionRequestEntity {
    @Id private String id;
    private String employeeId;
    private String employeeCodeSnapshot;
    private String employeeNameSnapshot;
    private String departmentIdSnapshot;
    private LocalDate attendanceDate;
    private MissingPunchType missingPunch;
    private Instant requestedCheckInAt;
    private Instant requestedCheckOutAt;
    private String reason;
    private AttendanceCorrectionStatus status;
    private String adminResponse;
    private String reviewedBy;
    private Instant reviewedAt;
    private String originalAttendanceId;
    private long originalAttendanceVersion;
    @Version private long version;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public String getId(){return id;} public void setId(String v){id=v;} public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getEmployeeCodeSnapshot(){return employeeCodeSnapshot;} public void setEmployeeCodeSnapshot(String v){employeeCodeSnapshot=v;} public String getEmployeeNameSnapshot(){return employeeNameSnapshot;} public void setEmployeeNameSnapshot(String v){employeeNameSnapshot=v;}
    public String getDepartmentIdSnapshot(){return departmentIdSnapshot;} public void setDepartmentIdSnapshot(String v){departmentIdSnapshot=v;} public LocalDate getAttendanceDate(){return attendanceDate;} public void setAttendanceDate(LocalDate v){attendanceDate=v;}
    public MissingPunchType getMissingPunch(){return missingPunch;} public void setMissingPunch(MissingPunchType v){missingPunch=v;} public Instant getRequestedCheckInAt(){return requestedCheckInAt;} public void setRequestedCheckInAt(Instant v){requestedCheckInAt=v;}
    public Instant getRequestedCheckOutAt(){return requestedCheckOutAt;} public void setRequestedCheckOutAt(Instant v){requestedCheckOutAt=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public AttendanceCorrectionStatus getStatus(){return status;} public void setStatus(AttendanceCorrectionStatus v){status=v;} public String getAdminResponse(){return adminResponse;} public void setAdminResponse(String v){adminResponse=v;}
    public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=v;} public Instant getReviewedAt(){return reviewedAt;} public void setReviewedAt(Instant v){reviewedAt=v;}
    public String getOriginalAttendanceId(){return originalAttendanceId;} public void setOriginalAttendanceId(String v){originalAttendanceId=v;} public long getOriginalAttendanceVersion(){return originalAttendanceVersion;} public void setOriginalAttendanceVersion(long v){originalAttendanceVersion=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
