package com.hrms.payroll.dto;
import com.hrms.payroll.model.PayrollRecord;
import org.springframework.data.domain.Page;
import java.util.List;
public record PayrollPageResponse(List<PayrollResponse> items,int page,int limit,long totalItems,int totalPages,boolean hasNext,boolean hasPrevious){
 public static PayrollPageResponse from(Page<PayrollRecord> p,int limit){return new PayrollPageResponse(p.getContent().stream().map(PayrollResponse::from).toList(),p.getNumber(),limit,p.getTotalElements(),p.getTotalPages(),p.hasNext(),p.hasPrevious());}
}
