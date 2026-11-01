package com.hrms.leave.repository;

import com.hrms.leave.model.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface LeaveBalanceRepository extends MongoRepository<LeaveBalance, String> {
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(String employeeId, LeaveType leaveType, int year);
    List<LeaveBalance> findByEmployeeIdAndYearOrderByLeaveTypeAsc(String employeeId, int year);
}
