package com.hrms.leave.repository;

import com.hrms.leave.model.*;
import org.springframework.data.mongodb.repository.*;
import java.time.LocalDate;
import java.util.*;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String>, LeaveRequestRepositoryCustom {
    @Query(value = "{'employeeId':?0,'status':{$in:?1},'startDate':{$lte:?3},'endDate':{$gte:?2}}", exists = true)
    boolean existsOverlap(String employeeId, Collection<LeaveStatus> statuses, LocalDate startDate, LocalDate endDate);

    @Query("{'employeeId':?0,'status':'APPROVED','startDate':{$lte:?2},'endDate':{$gte:?1}}")
    List<LeaveRequest> findApprovedOverlapping(String employeeId, LocalDate startDate, LocalDate endDate);
}
