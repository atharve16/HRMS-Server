package com.hrms.email;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

public record BrevoEmailMessage(
        List<Recipient> to,
        List<Recipient> cc,
        List<Recipient> bcc,
        Recipient replyTo,
        String subject,
        String htmlContent,
        String textContent,
        List<Attachment> attachments
) {
    public BrevoEmailMessage {
        to = List.copyOf(Objects.requireNonNull(to, "to"));
        cc = cc == null ? List.of() : List.copyOf(cc);
        bcc = bcc == null ? List.of() : List.copyOf(bcc);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        if (to.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        if ((htmlContent == null || htmlContent.isBlank())
                && (textContent == null || textContent.isBlank())) {
            throw new IllegalArgumentException("HTML or plain-text email content is required");
        }
    }

    public static BrevoEmailMessage html(String recipient, String subject, String htmlContent) {
        return new BrevoEmailMessage(List.of(new Recipient(recipient, null)), List.of(), List.of(), null,
                subject, htmlContent, null, List.of());
    }

    public static BrevoEmailMessage text(String recipient, String subject, String textContent) {
        return new BrevoEmailMessage(List.of(new Recipient(recipient, null)), List.of(), List.of(), null,
                subject, null, textContent, List.of());
    }

    public record Recipient(String email, String name) {
        public Recipient {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Recipient email is required");
            }
        }
    }

    public record Attachment(String name, String content) {
        public Attachment {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Attachment name is required");
            }
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Base64 attachment content is required");
            }
        }

        public static Attachment fromBytes(String name, byte[] content) {
            Objects.requireNonNull(content, "content");
            return new Attachment(name, Base64.getEncoder().encodeToString(content));
        }
    }
}
