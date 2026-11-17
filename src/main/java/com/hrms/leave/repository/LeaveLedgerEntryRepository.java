package com.hrms.leave.repository;

import com.hrms.leave.model.LeaveLedgerEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface LeaveLedgerEntryRepository extends MongoRepository<LeaveLedgerEntry, String> {
    List<LeaveLedgerEntry> findByEmployeeIdAndPeriodOrderByCreatedAtDesc(String employeeId, String period);
    Optional<LeaveLedgerEntry> findByIdempotencyKey(String idempotencyKey);
}
