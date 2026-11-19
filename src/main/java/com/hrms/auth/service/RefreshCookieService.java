package com.hrms.auth.service;

import com.hrms.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieService {

    private final AuthProperties properties;

    public RefreshCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String rawToken) {
        return create(rawToken, properties.jwt().refreshExpiry());
    }

    public ResponseCookie create(String rawToken, Duration maxAge) {
        Duration effectiveMaxAge = maxAge != null && !maxAge.isNegative()
                ? maxAge
                : Duration.ZERO;
        return base(rawToken).maxAge(effectiveMaxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String cookieName() {
        return properties.cookie().refreshTokenName();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.cookie().refreshTokenName(), value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(properties.cookie().path());
        if (properties.cookie().domain() != null && !properties.cookie().domain().isBlank()) {
            builder.domain(properties.cookie().domain());
        }
        return builder;
    }
}
