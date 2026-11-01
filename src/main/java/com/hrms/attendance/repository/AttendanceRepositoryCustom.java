package com.hrms.attendance.repository;
import com.hrms.attendance.dto.AttendanceSearch;import com.hrms.attendance.model.AttendanceRecord;import org.springframework.data.domain.Page;
public interface AttendanceRepositoryCustom { Page<AttendanceRecord> search(AttendanceSearch search); }
