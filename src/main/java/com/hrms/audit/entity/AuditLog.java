package com.hrms.audit.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("audit_logs")
@CompoundIndex(name = "audit_entity_created_idx", def = "{'entityType': 1, 'entityId': 1, 'createdAt': -1}")
public class AuditLog {
    @Id private String id;
    @Indexed private String actorId;
    private String actorRole;
    private String action;
    @Indexed private String entityType;
    @Indexed private String entityId;
    private Map<String, Object> before;
    private Map<String, Object> after;
    private String reason;
    @Indexed private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public Map<String, Object> getBefore() { return before; }
    public void setBefore(Map<String, Object> before) { this.before = before; }
    public Map<String, Object> getAfter() { return after; }
    public void setAfter(Map<String, Object> after) { this.after = after; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
