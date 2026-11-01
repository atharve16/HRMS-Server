package com.hrms.auth.security;

import com.hrms.auth.AuthTestProperties;
import com.hrms.auth.entity.User;
import com.hrms.config.AuthProperties;
import com.hrms.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void issuesAndValidatesRequiredIdentityClaims() {
        AuthProperties properties = AuthTestProperties.create();
        JwtConfig config = new JwtConfig();
        Instant now = Instant.now();
        JwtService service = new JwtService(config.jwtEncoder(properties), config.jwtDecoder(properties),
                properties, Clock.fixed(now, ZoneOffset.UTC));
        User user = User.initialAdmin("admin@example.com");
        user.setId("user-123");

        var decoded = service.decode(service.issueAccessToken(user));

        assertThat(decoded.getSubject()).isEqualTo("user-123");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("hrms-test");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("admin@example.com");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void rejectsExpiredAndIncorrectlySignedTokens() {
        AuthProperties properties = AuthTestProperties.create();
        JwtConfig config = new JwtConfig();
        User user = User.initialAdmin("admin@example.com");
        user.setId("user-123");
        JwtService expiredIssuer = new JwtService(config.jwtEncoder(properties), config.jwtDecoder(properties),
                properties, Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC));
        String expired = expiredIssuer.issueAccessToken(user);
        assertThatThrownBy(() -> expiredIssuer.decode(expired)).isInstanceOf(JwtException.class);

        AuthProperties other = withJwtSecret(properties,
                "different-signing-secret-at-least-thirty-two-chars");
        JwtService validIssuer = new JwtService(config.jwtEncoder(properties), config.jwtDecoder(properties),
                properties, Clock.systemUTC());
        JwtService wrongVerifier = new JwtService(config.jwtEncoder(other), config.jwtDecoder(other),
                other, Clock.systemUTC());
        String valid = validIssuer.issueAccessToken(user);
        assertThatThrownBy(() -> wrongVerifier.decode(valid)).isInstanceOf(JwtException.class);
    }

    private AuthProperties withJwtSecret(AuthProperties base, String secret) {
        AuthProperties.Jwt jwt = new AuthProperties.Jwt(secret, base.jwt().issuer(),
                base.jwt().accessExpiry(), base.jwt().refreshExpiry());
        return new AuthProperties(jwt, base.otp(), base.cookie(), base.mail(), base.rateLimit(), base.bootstrap());
    }
}
