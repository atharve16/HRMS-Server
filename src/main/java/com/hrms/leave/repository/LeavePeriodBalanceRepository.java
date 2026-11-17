package com.hrms.leave.repository;

import com.hrms.leave.model.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface LeavePeriodBalanceRepository extends MongoRepository<LeavePeriodBalance, String> {
    Optional<LeavePeriodBalance> findByEmployeeIdAndLeaveTypeAndPeriod(String employeeId, LeaveType leaveType, String period);
    List<LeavePeriodBalance> findByEmployeeIdAndPeriodOrderByLeaveTypeAsc(String employeeId, String period);
    List<LeavePeriodBalance> findByPeriod(String period);
    List<LeavePeriodBalance> findByEmployeeId(String employeeId);
}
