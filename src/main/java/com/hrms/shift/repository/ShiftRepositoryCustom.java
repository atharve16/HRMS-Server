package com.hrms.shift.repository;
import com.hrms.shift.model.Shift;
import org.springframework.data.domain.Page;
public interface ShiftRepositoryCustom { Page<Shift> search(ShiftSearch search); }
