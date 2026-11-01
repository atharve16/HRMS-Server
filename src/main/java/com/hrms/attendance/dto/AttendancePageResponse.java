package com.hrms.attendance.dto;
import com.hrms.attendance.model.AttendanceRecord;import org.springframework.data.domain.Page;import java.util.List;
public record AttendancePageResponse(List<AttendanceResponse> content,int page,int limit,long totalElements,int totalPages,boolean first,boolean last){public static AttendancePageResponse from(Page<AttendanceRecord> p,int l){return new AttendancePageResponse(p.getContent().stream().map(AttendanceResponse::from).toList(),p.getNumber(),l,p.getTotalElements(),p.getTotalPages(),p.isFirst(),p.isLast());}}
