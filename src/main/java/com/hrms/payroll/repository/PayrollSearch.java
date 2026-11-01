package com.hrms.payroll.repository;
import com.hrms.payroll.model.PayrollStatus;
public record PayrollSearch(Integer month,Integer year,String employeeId,String employeeCode,
                            String departmentId,PayrollStatus status,int page,int limit,String sortDirection) {}
