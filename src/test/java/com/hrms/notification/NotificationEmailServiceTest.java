package com.hrms.notification;

import com.hrms.email.BrevoEmailClient;
import com.hrms.email.BrevoEmailException;
import com.hrms.email.BrevoEmailMessage;
import com.hrms.notification.service.NotificationEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class NotificationEmailServiceTest {
    @Test
    void mailFailureIsLoggedAndNeverEscapesBusinessFlow() {
        BrevoEmailClient client = mock(BrevoEmailClient.class);
        when(client.send(any())).thenThrow(BrevoEmailException.network(new java.io.IOException("unavailable")));
        NotificationEmailService service = new NotificationEmailService(client);

        assertThatCode(() -> service.sendHolidayAnnouncement("e@example.com", "Holiday", LocalDate.of(2030, 1, 1)))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.sendLeaveDecision("e@example.com", true, "CL",
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 2), null)).doesNotThrowAnyException();
    }

    @Test
    void preservesHolidayAndLeaveSubjectsAndPlainTextContent() {
        BrevoEmailClient client = mock(BrevoEmailClient.class);
        when(client.send(any())).thenReturn("message-id");
        NotificationEmailService service = new NotificationEmailService(client);

        service.sendHolidayAnnouncement("employee@example.com", "Republic Day", LocalDate.of(2030, 1, 26));
        service.sendLeaveDecision("employee@example.com", false, "CL",
                LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 2), "Insufficient balance");

        ArgumentCaptor<BrevoEmailMessage> messages = ArgumentCaptor.forClass(BrevoEmailMessage.class);
        verify(client, times(2)).send(messages.capture());
        org.assertj.core.api.Assertions.assertThat(messages.getAllValues().get(0).subject())
                .isEqualTo("Company holiday: Republic Day");
        org.assertj.core.api.Assertions.assertThat(messages.getAllValues().get(0).textContent())
                .contains("Republic Day", "2030-01-26", "paid company holiday");
        org.assertj.core.api.Assertions.assertThat(messages.getAllValues().get(1).subject())
                .isEqualTo("Leave request rejected");
        org.assertj.core.api.Assertions.assertThat(messages.getAllValues().get(1).textContent())
                .contains("CL", "2030-02-01", "2030-02-02", "rejected", "Insufficient balance");
    }
}
