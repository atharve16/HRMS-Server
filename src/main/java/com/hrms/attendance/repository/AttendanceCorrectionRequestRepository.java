package com.hrms.attendance.repository;

import com.hrms.attendance.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.*;

public interface AttendanceCorrectionRequestRepository extends MongoRepository<AttendanceCorrectionRequestEntity, String> {
    boolean existsByEmployeeIdAndAttendanceDateAndStatus(String employeeId, LocalDate date, AttendanceCorrectionStatus status);
    List<AttendanceCorrectionRequestEntity> findByEmployeeIdOrderByAttendanceDateDescCreatedAtDesc(String employeeId);
    Page<AttendanceCorrectionRequestEntity> findAllByOrderByAttendanceDateDescCreatedAtDesc(Pageable pageable);
}
