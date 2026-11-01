package com.hrms.auth.repository;

import com.hrms.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
        "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
        "app.auth.mail.from=noreply@example.com",
        "management.health.mongo.enabled=false"
})
class AuthMongoIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));

    @DynamicPropertySource
    static void mongodbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "hrms_auth_test");
    }

    @Autowired private UserRepository users;
    @Autowired private TransactionTemplate transactions;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void rejectsDuplicateNormalizedEmailAtDatabaseBoundary() {
        User first = User.initialAdmin("admin@example.com");
        first.setInitialAdminMarker(null);
        users.save(first);
        User duplicate = User.initialAdmin("admin@example.com");
        duplicate.setInitialAdminMarker(null);

        assertThatThrownBy(() -> users.save(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void rollsBackRelatedWritesInReplicaSetTransaction() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            User user = User.initialAdmin("rollback@example.com");
            user.setInitialAdminMarker(null);
            users.save(user);
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(users.findByNormalizedEmail("rollback@example.com")).isEmpty();
    }
}
