package com.hrms.leave.repository;

import com.hrms.leave.model.LeaveRolloverRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface LeaveRolloverRecordRepository extends MongoRepository<LeaveRolloverRecord, String> {
    Optional<LeaveRolloverRecord> findByEmployeeIdAndPeriod(String employeeId, String period);
}
