package com.hrms.office.repository;
import com.hrms.office.model.OfficeLocation;
import org.springframework.data.domain.Page;
public interface OfficeLocationRepositoryCustom { Page<OfficeLocation> search(OfficeSearch search); }
