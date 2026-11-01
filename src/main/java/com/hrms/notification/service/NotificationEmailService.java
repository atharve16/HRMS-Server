package com.hrms.notification.service;

import com.hrms.config.AuthProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
public class NotificationEmailService {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);
    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    public NotificationEmailService(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.mail().from(), properties.mail().fromName());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Notification email delivery failed for recipient {} and subject {}", recipient, subject, exception);
        }
    }
}
