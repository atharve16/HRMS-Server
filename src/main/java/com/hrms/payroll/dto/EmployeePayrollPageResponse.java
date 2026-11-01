package com.hrms.payroll.dto;
import com.hrms.payroll.model.PayrollRecord;
import org.springframework.data.domain.Page;
import java.util.List;
public record EmployeePayrollPageResponse(List<EmployeePayrollResponse> items,int page,int limit,long totalItems,int totalPages,boolean hasNext,boolean hasPrevious){
 public static EmployeePayrollPageResponse from(Page<PayrollRecord> p,int limit){return new EmployeePayrollPageResponse(p.getContent().stream().map(EmployeePayrollResponse::from).toList(),p.getNumber(),limit,p.getTotalElements(),p.getTotalPages(),p.hasNext(),p.hasPrevious());}
}
