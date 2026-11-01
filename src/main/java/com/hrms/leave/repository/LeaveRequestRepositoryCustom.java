package com.hrms.leave.repository;

import com.hrms.leave.dto.LeaveSearch;
import com.hrms.leave.model.LeaveRequest;
import org.springframework.data.domain.Page;

public interface LeaveRequestRepositoryCustom {
    Page<LeaveRequest> search(LeaveSearch search);
}
