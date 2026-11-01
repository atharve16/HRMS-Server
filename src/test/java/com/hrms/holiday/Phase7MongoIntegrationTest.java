package com.hrms.holiday;

import com.hrms.holiday.dto.HolidaySearch;
import com.hrms.holiday.model.CompanyHoliday;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.notification.model.*;
import com.hrms.notification.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.auth.jwt.secret=jwt-test-secret-at-least-thirty-two-characters",
        "app.auth.otp.hash-secret=otp-test-secret-at-least-thirty-two-characters",
        "app.auth.mail.from=noreply@example.com", "spring.data.mongodb.auto-index-creation=true",
        "management.health.mongo.enabled=false"})
class Phase7MongoIntegrationTest {
    @Container static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.16"));
    @DynamicPropertySource static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "hrms_phase7_test");
    }
    @Autowired CompanyHolidayRepository holidays;
    @Autowired NotificationRepository notifications;
    @Autowired MongoTemplate template;

    @BeforeEach void clean() { notifications.deleteAll(); holidays.deleteAll(); }

    @Test
    void createsIndexesAndEnforcesUniqueActiveHolidayDate() {
        assertThat(template.indexOps(CompanyHoliday.class).getIndexInfo().stream().map(i -> i.getName()).toList())
                .contains("active_holiday_date_unique", "holiday_date_active_idx");
        assertThat(template.indexOps(Notification.class).getIndexInfo().stream().map(i -> i.getName()).toList())
                .contains("notification_user_created_idx", "notification_user_read_created_idx");

        LocalDate date = LocalDate.of(2030, 1, 26);
        CompanyHoliday first = holiday("507f1f77bcf86cd799439001", date, true);
        holidays.save(first);
        assertThatThrownBy(() -> holidays.save(holiday("507f1f77bcf86cd799439002", date, true)))
                .isInstanceOf(DuplicateKeyException.class);
        first.setActive(false); holidays.save(first);
        holidays.save(holiday("507f1f77bcf86cd799439003", date, true));
        assertThat(holidays.findAll()).hasSize(2);
    }

    @Test
    void filtersHolidaysAndScopesNotificationQueriesByUser() {
        holidays.save(holiday("507f1f77bcf86cd799439001", LocalDate.of(2030, 1, 26), true));
        holidays.save(holiday("507f1f77bcf86cd799439002", LocalDate.of(2031, 1, 26), false));
        var page = holidays.search(new HolidaySearch(null, LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 12, 31), true, 0, 10, "asc"));
        assertThat(page.getTotalElements()).isEqualTo(1);

        notifications.save(notification("507f1f77bcf86cd799439010", "user-a", false));
        notifications.save(notification("507f1f77bcf86cd799439011", "user-b", false));
        assertThat(notifications.countByUserIdAndReadFalse("user-a")).isEqualTo(1);
        assertThat(notifications.findByUserId("user-a", PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()).extracting(Notification::getUserId)
                .containsOnly("user-a");
        notifications.markAllRead("user-a", Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(notifications.countByUserIdAndReadFalse("user-a")).isZero();
        assertThat(notifications.countByUserIdAndReadFalse("user-b")).isEqualTo(1);
    }

    private static CompanyHoliday holiday(String id, LocalDate date, boolean active) {
        CompanyHoliday h = new CompanyHoliday(); h.setId(id); h.setName("Holiday"); h.setDate(date); h.setActive(active); return h;
    }
    private static Notification notification(String id, String userId, boolean read) {
        Notification n = new Notification(); n.setId(id); n.setUserId(userId); n.setTitle("Title"); n.setMessage("Message");
        n.setType(NotificationType.HOLIDAY_ANNOUNCEMENT); n.setRead(read); n.setCreatedAt(Instant.now()); return n;
    }
}
