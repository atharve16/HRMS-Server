package com.hrms.notification.service;

import com.hrms.email.BrevoEmailClient;
import com.hrms.email.BrevoEmailException;
import com.hrms.email.BrevoEmailMessage;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class NotificationEmailService {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);
    private final BrevoEmailClient emailClient;

    public NotificationEmailService(BrevoEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void sendHolidayAnnouncement(String recipient, String holidayName, LocalDate date) {
        send(recipient, "Company holiday: " + holidayName,
                "A company holiday has been declared for %s on %s. This is a paid company holiday."
                        .formatted(holidayName, date));
    }

    public void sendLeaveDecision(String recipient, boolean approved, String leaveType,
                                  LocalDate startDate, LocalDate endDate, String remarks) {
        String decision = approved ? "approved" : "rejected";
        String text = "Your %s leave request from %s to %s has been %s."
                .formatted(leaveType, startDate, endDate, decision);
        if (!approved && remarks != null && !remarks.isBlank()) text += " Remarks: " + remarks;
        send(recipient, "Leave request " + decision, text);
    }

    private void send(String recipient, String subject, String body) {
        try {
            emailClient.send(BrevoEmailMessage.text(recipient, subject, body));
        } catch (BrevoEmailException exception) {
            log.error("Notification email delivery failed (category={}, status={})",
                    exception.getCategory(), exception.getStatusCode());
        } catch (RuntimeException exception) {
            // Preserve the post-commit, best-effort contract even for an unexpected client-side failure.
            log.error("Notification email delivery failed before Brevo returned a response");
        }
    }
}
