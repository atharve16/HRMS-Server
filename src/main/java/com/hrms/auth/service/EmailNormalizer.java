package com.hrms.auth.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
