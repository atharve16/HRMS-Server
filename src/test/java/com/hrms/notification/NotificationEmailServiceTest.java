package com.hrms.notification;

import com.hrms.config.AuthProperties;
import com.hrms.notification.service.NotificationEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class NotificationEmailServiceTest {
    @Test
    void mailFailureIsLoggedAndNeverEscapesBusinessFlow() {
        JavaMailSender sender = mock(JavaMailSender.class);
        AuthProperties properties = mock(AuthProperties.class);
        when(properties.mail()).thenReturn(new AuthProperties.Mail("noreply@example.com", "HRMS"));
        when(sender.createMimeMessage()).thenThrow(new MailSendException("SMTP unavailable"));
        NotificationEmailService service = new NotificationEmailService(sender, properties);

        assertThatCode(() -> service.sendHolidayAnnouncement("e@example.com", "Holiday", LocalDate.of(2030, 1, 1)))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.sendLeaveDecision("e@example.com", true, "CL",
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 2), null)).doesNotThrowAnyException();
    }
}
