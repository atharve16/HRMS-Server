package com.hrms.email;

import com.hrms.auth.AuthTestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrevoEmailClientTest {
    private static final String URL = "https://api.brevo.test/v3/smtp/email";
    private MockRestServiceServer server;
    private BrevoEmailClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader("api-key", "test-api-key")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BrevoEmailClient(builder.build(), properties(), AuthTestProperties.create());
    }

    @Test
    void submitsHtmlEmailAndReturnsBrevoMessageId() {
        server.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value("noreply@example.com"))
                .andExpect(jsonPath("$.sender.name").value("HRMS Test"))
                .andExpect(jsonPath("$.to[0].email").value("employee@example.com"))
                .andExpect(jsonPath("$.subject").value("Subject"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Hello</p>"))
                .andRespond(withSuccess("{\"messageId\":\"brevo-message-id\"}", MediaType.APPLICATION_JSON));

        String messageId = client.send(BrevoEmailMessage.html(
                "employee@example.com", "Subject", "<p>Hello</p>"));

        assertThat(messageId).isEqualTo("brevo-message-id");
        server.verify();
    }

    @Test
    void serializesMultipleRecipientsCcBccReplyToAndAttachments() {
        server.expect(once(), requestTo(URL))
                .andExpect(jsonPath("$.to.length()").value(2))
                .andExpect(jsonPath("$.cc[0].email").value("cc@example.com"))
                .andExpect(jsonPath("$.bcc[0].email").value("bcc@example.com"))
                .andExpect(jsonPath("$.replyTo.email").value("reply@example.com"))
                .andExpect(jsonPath("$.textContent").value("Plain text"))
                .andExpect(jsonPath("$.attachment[0].name").value("note.txt"))
                .andExpect(jsonPath("$.attachment[0].content").value("aGVsbG8="))
                .andRespond(withSuccess("{\"messageId\":\"advanced-message-id\"}", MediaType.APPLICATION_JSON));
        BrevoEmailMessage message = new BrevoEmailMessage(
                List.of(new BrevoEmailMessage.Recipient("one@example.com", "One"),
                        new BrevoEmailMessage.Recipient("two@example.com", null)),
                List.of(new BrevoEmailMessage.Recipient("cc@example.com", null)),
                List.of(new BrevoEmailMessage.Recipient("bcc@example.com", null)),
                new BrevoEmailMessage.Recipient("reply@example.com", "Support"),
                "Subject", null, "Plain text",
                List.of(BrevoEmailMessage.Attachment.fromBytes("note.txt", "hello".getBytes())));

        assertThat(client.send(message)).isEqualTo("advanced-message-id");
        server.verify();
    }

    @Test
    void classifiesInvalidApiKey() {
        expectError(HttpStatus.UNAUTHORIZED, "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}");
        assertFailure(BrevoEmailException.Category.AUTHENTICATION, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void classifiesInvalidSender() {
        expectError(HttpStatus.BAD_REQUEST, "{\"code\":\"invalid_parameter\",\"message\":\"Invalid sender\"}");
        assertFailure(BrevoEmailException.Category.VALIDATION, HttpStatus.BAD_REQUEST);
    }

    @Test
    void classifiesInvalidRecipient() {
        expectError(HttpStatus.BAD_REQUEST, "{\"code\":\"invalid_parameter\",\"message\":\"Invalid to email\"}");
        assertFailure(BrevoEmailException.Category.VALIDATION, HttpStatus.BAD_REQUEST);
    }

    @Test
    void classifiesBrevoApiErrors() {
        expectError(HttpStatus.INTERNAL_SERVER_ERROR,
                "{\"code\":\"internal_error\",\"message\":\"Temporary failure\"}");
        assertFailure(BrevoEmailException.Category.API, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void classifiesNetworkFailures() {
        server.expect(once(), requestTo(URL)).andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.send(message()))
                .isInstanceOfSatisfying(BrevoEmailException.class, exception -> {
                    assertThat(exception.getCategory()).isEqualTo(BrevoEmailException.Category.NETWORK);
                    assertThat(exception.getStatusCode()).isNull();
                    assertThat(exception.getMessage()).doesNotContain("test-api-key", "employee@example.com", "Hello");
                });
        server.verify();
    }

    private void expectError(HttpStatus status, String responseBody) {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(status)
                .contentType(MediaType.APPLICATION_JSON).body(responseBody));
    }

    private void assertFailure(BrevoEmailException.Category category, HttpStatus status) {
        assertThatThrownBy(() -> client.send(message()))
                .isInstanceOfSatisfying(BrevoEmailException.class, exception -> {
                    assertThat(exception.getCategory()).isEqualTo(category);
                    assertThat(exception.getStatusCode()).isEqualTo(status);
                    assertThat(exception.getMessage()).doesNotContain("test-api-key", "employee@example.com", "Hello");
                });
        server.verify();
    }

    private BrevoEmailMessage message() {
        return BrevoEmailMessage.text("employee@example.com", "Subject", "Hello");
    }

    private BrevoProperties properties() {
        return new BrevoProperties(URL, "test-api-key", Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
