package com.hrms.leave.repository;

import com.hrms.leave.model.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface LeavePolicyRepository extends MongoRepository<LeavePolicy, String> {
    Optional<LeavePolicy> findByScopeAndEmployeeIdAndLeaveType(LeavePolicyScope scope, String employeeId, LeaveType leaveType);
    List<LeavePolicy> findByScopeAndEmployeeId(LeavePolicyScope scope, String employeeId);
    List<LeavePolicy> findByScope(LeavePolicyScope scope);
}
