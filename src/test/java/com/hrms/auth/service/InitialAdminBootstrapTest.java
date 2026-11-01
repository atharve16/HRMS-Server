package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.repository.UserRepository;
import com.hrms.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialAdminBootstrapTest {

    @Test
    void skipsBootstrapWhenConfigurationIsMissing() {
        UserRepository repository = mock(UserRepository.class);
        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(repository, new EmailNormalizer(),
                AuthTestProperties.create());

        bootstrap.run(new DefaultApplicationArguments());

        verify(repository, never()).save(any());
    }

    @Test
    void createsOnlyOneNormalizedInitialAdmin() {
        UserRepository repository = mock(UserRepository.class);
        AuthProperties base = AuthTestProperties.create();
        AuthProperties configured = new AuthProperties(base.jwt(), base.otp(), base.cookie(), base.mail(),
                base.rateLimit(), new AuthProperties.Bootstrap(" Admin@Example.COM "));
        when(repository.existsByRole(UserRole.ADMIN)).thenReturn(false, true);
        when(repository.findByNormalizedEmail("admin@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(repository, new EmailNormalizer(), configured);

        bootstrap.run(new DefaultApplicationArguments());
        bootstrap.run(new DefaultApplicationArguments());

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getNormalizedEmail().equals("admin@example.com") && user.getRole() == UserRole.ADMIN));
    }

    @Test
    void neverPromotesExistingEmployeeAndToleratesConcurrentWinner() {
        UserRepository repository = mock(UserRepository.class);
        AuthProperties base = AuthTestProperties.create();
        AuthProperties configured = new AuthProperties(base.jwt(), base.otp(), base.cookie(), base.mail(),
                base.rateLimit(), new AuthProperties.Bootstrap("admin@example.com"));
        when(repository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(repository.findByNormalizedEmail("admin@example.com"))
                .thenReturn(Optional.of(new User()), Optional.empty());
        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(repository, new EmailNormalizer(), configured);

        bootstrap.run(new DefaultApplicationArguments());
        verify(repository, never()).save(any());

        when(repository.findByNormalizedEmail("admin@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(new DuplicateKeyException("concurrent"));
        bootstrap.run(new DefaultApplicationArguments());
    }
}
