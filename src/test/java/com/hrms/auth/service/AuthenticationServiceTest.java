package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.security.JwtService;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Mock private UserRepository users;
    @Mock private OtpService otpService;
    @Mock private RefreshTokenService refreshTokens;
    @Mock private JwtService jwtService;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthenticationService(users, otpService, refreshTokens, jwtService,
                new EmailNormalizer(), AuthTestProperties.create(),
                Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void authenticatesActiveUserAndReturnsOnlyRawRefreshTokenToTransportLayer() {
        User user = User.initialAdmin("admin@example.com");
        user.setId("user-id");
        RefreshToken document = new RefreshToken();
        when(users.findByNormalizedEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(refreshTokens.issue("user-id"))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", document));
        when(jwtService.issueAccessToken(user)).thenReturn("signed-access");

        AuthenticationService.AuthResult result = service.verifyOtp(" Admin@Example.COM ", "123456");

        verify(otpService).verify("admin@example.com", "123456");
        assertThat(result.response().accessToken()).isEqualTo("signed-access");
        assertThat(result.rawRefreshToken()).isEqualTo("raw-refresh");
        assertThat(result.response().user().role().name()).isEqualTo("ADMIN");
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void rejectsInactiveUserBeforeConsumingOtp() {
        User user = User.initialAdmin("inactive@example.com");
        user.setStatus(UserStatus.INACTIVE);
        when(users.findByNormalizedEmail("inactive@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyOtp("inactive@example.com", "123456"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_INACTIVE));
        verify(otpService, never()).verify("inactive@example.com", "123456");
    }

    @Test
    void doesNotDiscloseUnregisteredUserDuringOtpRequest() {
        service.requestOtp(" Missing@Example.com ");

        verify(otpService).issue("missing@example.com");
    }
}
