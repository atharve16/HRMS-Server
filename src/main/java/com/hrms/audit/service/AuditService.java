package com.hrms.audit.service;

import com.hrms.audit.entity.AuditLog;
import com.hrms.audit.repository.AuditLogRepository;
import com.hrms.auth.security.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(AuthPrincipal actor, String action, String entityType, String entityId,
                       Map<String, Object> before, Map<String, Object> after, String reason) {
        AuditLog log = new AuditLog();
        log.setActorId(actor.userId());
        log.setActorRole(actor.role().name());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBefore(before);
        log.setAfter(after);
        log.setReason(reason);
        log.setCreatedAt(clock.instant());
        repository.save(log);
    }
}
