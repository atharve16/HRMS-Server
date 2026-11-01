package com.hrms.auth.service;

import com.hrms.config.AuthProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class OtpHashService {

    private final SecretKeySpec key;

    public OtpHashService(AuthProperties properties) {
        this.key = new SecretKeySpec(
                properties.otp().hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String hash(String normalizedEmail, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal((normalizedEmail + ':' + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("OTP hashing is unavailable", exception);
        }
    }

    public boolean matches(String normalizedEmail, String otp, String expectedHash) {
        byte[] actual = hash(normalizedEmail, otp).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
