package com.hrms.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hrms.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class BrevoEmailClient {
    private static final Logger log = LoggerFactory.getLogger(BrevoEmailClient.class);

    private final RestClient restClient;
    private final BrevoProperties properties;
    private final AuthProperties authProperties;

    public BrevoEmailClient(RestClient brevoRestClient, BrevoProperties properties,
                            AuthProperties authProperties) {
        this.restClient = brevoRestClient;
        this.properties = properties;
        this.authProperties = authProperties;
    }

    public String send(BrevoEmailMessage message) {
        AuthProperties.Mail configuredSender = authProperties.mail();
        Request request = new Request(
                new Contact(configuredSender.from(), configuredSender.fromName()),
                contacts(message.to()), contacts(message.cc()), contacts(message.bcc()),
                contact(message.replyTo()), message.subject(), message.htmlContent(), message.textContent(),
                attachments(message.attachments()));
        try {
            Response response = restClient.post()
                    .uri(properties.url())
                    .body(request)
                    .retrieve()
                    .body(Response.class);
            if (response == null || response.messageId() == null || response.messageId().isBlank()) {
                throw BrevoEmailException.api("Brevo accepted the request without returning a message ID", null);
            }
            log.debug("Brevo accepted email submission with message ID {}", response.messageId());
            return response.messageId();
        } catch (HttpStatusCodeException exception) {
            HttpStatusCode status = exception.getStatusCode();
            BrevoEmailException.Category category = status.value() == 401 || status.value() == 403
                    ? BrevoEmailException.Category.AUTHENTICATION
                    : status.value() == 400 || status.value() == 422
                    ? BrevoEmailException.Category.VALIDATION
                    : BrevoEmailException.Category.API;
            throw BrevoEmailException.http(category, status, exception);
        } catch (ResourceAccessException exception) {
            throw BrevoEmailException.network(exception);
        } catch (BrevoEmailException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw BrevoEmailException.api("Brevo email submission failed before a valid response was received",
                    exception);
        }
    }

    private List<Contact> contacts(List<BrevoEmailMessage.Recipient> recipients) {
        return recipients.stream().map(this::contact).toList();
    }

    private Contact contact(BrevoEmailMessage.Recipient recipient) {
        return recipient == null ? null : new Contact(recipient.email(), recipient.name());
    }

    private List<Attachment> attachments(List<BrevoEmailMessage.Attachment> source) {
        return source.stream().map(attachment -> new Attachment(attachment.content(), attachment.name())).toList();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record Request(Contact sender, List<Contact> to, List<Contact> cc, List<Contact> bcc,
                           Contact replyTo, String subject, String htmlContent, String textContent,
                           List<Attachment> attachment) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private record Contact(String email, String name) {
    }

    private record Attachment(String content, String name) {
    }

    private record Response(String messageId) {
    }
}
