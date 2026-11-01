package com.hrms.auth.security;

import com.hrms.auth.entity.UserRole;

import java.security.Principal;

public record AuthPrincipal(String userId, String normalizedEmail, UserRole role) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}
