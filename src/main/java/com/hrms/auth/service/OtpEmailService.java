package com.hrms.auth.service;

import com.hrms.config.AuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

@Service
public class OtpEmailService {

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    public OtpEmailService(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendAuthenticationOtp(String recipient, String otp)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.mail().from(), properties.mail().fromName());
        helper.setTo(recipient);
        helper.setSubject("Your HRMS sign-in code");
        helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                  <h2>Sign in to HRMS</h2>
                  <p>Use the following one-time code to complete your sign-in:</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px">%s</p>
                  <p>This code expires in %d minutes. If you did not request it, you can ignore this email.</p>
                </div>
                """.formatted(otp, properties.otp().expiry().toMinutes()), true);
        mailSender.send(message);
    }
}
