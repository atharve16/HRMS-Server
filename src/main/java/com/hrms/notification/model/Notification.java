package com.hrms.notification.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document("notifications")
@CompoundIndexes({
        @CompoundIndex(name = "notification_user_created_idx", def = "{'userId':1,'createdAt':-1}"),
        @CompoundIndex(name = "notification_user_read_created_idx", def = "{'userId':1,'isRead':1,'createdAt':-1}")
})
public class Notification {
    @Id private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private String referenceId;
    @Field("isRead") private boolean read;
    private Instant readAt;
    @CreatedDate private Instant createdAt;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getUserId() { return userId; } public void setUserId(String v) { userId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getMessage() { return message; } public void setMessage(String v) { message = v; }
    public NotificationType getType() { return type; } public void setType(NotificationType v) { type = v; }
    public String getReferenceId() { return referenceId; } public void setReferenceId(String v) { referenceId = v; }
    public boolean isRead() { return read; } public void setRead(boolean v) { read = v; }
    public Instant getReadAt() { return readAt; } public void setReadAt(Instant v) { readAt = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
}
