package com.hrms.auth.service;

import com.hrms.config.AuthProperties;
import com.hrms.email.BrevoEmailClient;
import com.hrms.email.BrevoEmailMessage;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {

    private final BrevoEmailClient emailClient;
    private final AuthProperties properties;

    public OtpEmailService(BrevoEmailClient emailClient, AuthProperties properties) {
        this.emailClient = emailClient;
        this.properties = properties;
    }

    public void sendAuthenticationOtp(String recipient, String otp) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                  <h2>Sign in to HRMS</h2>
                  <p>Use the following one-time code to complete your sign-in:</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px">%s</p>
                  <p>This code expires in %d minutes. If you did not request it, you can ignore this email.</p>
                </div>
                """.formatted(otp, properties.otp().expiry().toMinutes());
        emailClient.send(BrevoEmailMessage.html(recipient, "Your HRMS sign-in code", html));
    }
}
