package com.hrms.notification.repository;

import com.hrms.notification.model.Notification;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {
    private final MongoTemplate template;
    public NotificationRepositoryImpl(MongoTemplate template) { this.template = template; }

    @Override
    public long markAllRead(String userId, Instant readAt) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("isRead").is(false));
        UpdateResult result = template.updateMulti(query, new Update().set("isRead", true).set("readAt", readAt),
                Notification.class);
        return result.getModifiedCount();
    }
}
