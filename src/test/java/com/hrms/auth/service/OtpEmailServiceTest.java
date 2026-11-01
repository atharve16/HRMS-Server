package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.email.BrevoEmailClient;
import com.hrms.email.BrevoEmailMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpEmailServiceTest {

    @Test
    void preservesOtpSubjectAndHtmlTemplate() {
        BrevoEmailClient client = mock(BrevoEmailClient.class);
        when(client.send(org.mockito.ArgumentMatchers.any())).thenReturn("message-id");
        OtpEmailService service = new OtpEmailService(client, AuthTestProperties.create());

        service.sendAuthenticationOtp("employee@example.com", "123456");

        ArgumentCaptor<BrevoEmailMessage> message = ArgumentCaptor.forClass(BrevoEmailMessage.class);
        verify(client).send(message.capture());
        assertThat(message.getValue().to()).containsExactly(
                new BrevoEmailMessage.Recipient("employee@example.com", null));
        assertThat(message.getValue().subject()).isEqualTo("Your HRMS sign-in code");
        assertThat(message.getValue().htmlContent())
                .contains("Sign in to HRMS", "123456", "expires in 5 minutes");
        assertThat(message.getValue().textContent()).isNull();
    }
}
