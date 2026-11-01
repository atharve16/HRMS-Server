package com.hrms.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
