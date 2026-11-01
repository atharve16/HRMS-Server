package com.hrms.auth.service;

import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.repository.UserRepository;
import com.hrms.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);
    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final AuthProperties properties;

    public InitialAdminBootstrap(UserRepository userRepository, EmailNormalizer emailNormalizer,
                                 AuthProperties properties) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configuredEmail = properties.bootstrap().initialAdminEmail();
        if (configuredEmail == null || configuredEmail.isBlank()) {
            log.info("Initial admin bootstrap skipped because INITIAL_ADMIN_EMAIL is not configured");
            return;
        }
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("Initial admin bootstrap skipped because an administrator already exists");
            return;
        }
        String normalizedEmail = emailNormalizer.normalize(configuredEmail);
        if (userRepository.findByNormalizedEmail(normalizedEmail).isPresent()) {
            log.warn("Initial admin bootstrap refused because the configured email belongs to an existing user");
            return;
        }
        try {
            User created = userRepository.save(User.initialAdmin(normalizedEmail));
            log.info("Initial administrator created with user id {}", created.getId());
        } catch (DuplicateKeyException exception) {
            log.info("Initial admin bootstrap lost a concurrent creation race; no additional admin was created");
        }
    }
}
