package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OtpSecurityTest {

    @Test
    void generatesSixDigitCodesWithSecureRandomness() {
        SecureTokenGenerator generator = new SecureTokenGenerator();
        Set<String> generated = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            generated.add(generator.generateOtp());
        }

        assertThat(generated).allMatch(value -> value.matches("\\d{6}"));
        assertThat(generated).hasSizeGreaterThan(90);
    }

    @Test
    void hashesOtpWithEmailBindingAndUsesSecureComparison() {
        OtpHashService service = new OtpHashService(AuthTestProperties.create());
        String hash = service.hash("person@example.com", "123456");

        assertThat(hash).doesNotContain("123456");
        assertThat(service.matches("person@example.com", "123456", hash)).isTrue();
        assertThat(service.matches("other@example.com", "123456", hash)).isFalse();
        assertThat(service.matches("person@example.com", "654321", hash)).isFalse();
    }

    @Test
    void generatesHighEntropyRefreshTokens() {
        SecureTokenGenerator generator = new SecureTokenGenerator();

        String first = generator.generateRefreshToken();
        String second = generator.generateRefreshToken();

        assertThat(first).hasSizeGreaterThanOrEqualTo(43).isNotEqualTo(second);
    }
}
