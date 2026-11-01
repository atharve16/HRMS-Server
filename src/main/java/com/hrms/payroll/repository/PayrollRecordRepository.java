package com.hrms.payroll.repository;
import com.hrms.payroll.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface PayrollRecordRepository extends MongoRepository<PayrollRecord,String>,PayrollRecordRepositoryCustom {
    boolean existsByEmployeeIdAndPayrollMonthAndPayrollYear(String employeeId,int month,int year);
    Optional<PayrollRecord> findByIdAndEmployeeIdAndStatusIn(String id,String employeeId,Collection<PayrollStatus> statuses);
    Page<PayrollRecord> findByEmployeeIdAndStatusIn(String employeeId,Collection<PayrollStatus> statuses,Pageable pageable);
    Page<PayrollRecord> findByEmployeeIdAndPayrollYearAndStatusIn(String employeeId,int year,Collection<PayrollStatus> statuses,Pageable pageable);
}
