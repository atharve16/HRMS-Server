package com.hrms.auth.security;

import com.hrms.auth.entity.User;
import com.hrms.config.AuthProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.jwt().accessExpiry()))
                .claim("email", user.getNormalizedEmail())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }
}
