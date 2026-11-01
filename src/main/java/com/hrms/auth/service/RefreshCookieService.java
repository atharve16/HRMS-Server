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
        return base(rawToken).maxAge(properties.jwt().refreshExpiry()).build();
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
