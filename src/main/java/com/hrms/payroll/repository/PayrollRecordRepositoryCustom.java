package com.hrms.payroll.repository;
import com.hrms.payroll.model.PayrollRecord;
import org.springframework.data.domain.Page;
public interface PayrollRecordRepositoryCustom { Page<PayrollRecord> search(PayrollSearch search); }
