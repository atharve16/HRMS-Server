package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.auth.entity.OtpVerification;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.OtpVerificationRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    @Mock private UserRepository users;
    @Mock private OtpVerificationRepository otps;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private OtpEmailService emailService;
    private OtpService service;
    private OtpHashService hashService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hashService = new OtpHashService(AuthTestProperties.create());
        service = new OtpService(users, otps, mongoTemplate, hashService,
                new SecureTokenGenerator(), emailService, AuthTestProperties.create(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doesNothingForUnregisteredOrInactiveUsers() throws Exception {
        when(users.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());
        User inactive = user("inactive@example.com", UserStatus.INACTIVE);
        when(users.findByNormalizedEmail("inactive@example.com")).thenReturn(Optional.of(inactive));

        service.issue("missing@example.com");
        service.issue("inactive@example.com");

        verify(emailService, never()).sendAuthenticationOtp(any(), any());
        verify(otps, never()).save(any());
    }

    @Test
    void enforcesResendCooldownWithoutSendingAnotherEmail() throws Exception {
        when(users.findByNormalizedEmail("active@example.com"))
                .thenReturn(Optional.of(user("active@example.com", UserStatus.ACTIVE)));
        OtpVerification latest = verification("active@example.com", "123456");
        latest.setResendAllowedAt(NOW.plusSeconds(30));
        when(otps.findTopByNormalizedEmailOrderByCreatedAtDesc("active@example.com"))
                .thenReturn(Optional.of(latest));

        service.issue("active@example.com");

        verify(emailService, never()).sendAuthenticationOtp(any(), any());
    }

    @Test
    void failedMailDeliveryDoesNotLeaveIssuedOtpUsable() throws Exception {
        when(users.findByNormalizedEmail("active@example.com"))
                .thenReturn(Optional.of(user("active@example.com", UserStatus.ACTIVE)));
        when(otps.findTopByNormalizedEmailOrderByCreatedAtDesc("active@example.com"))
                .thenReturn(Optional.empty());
        when(otps.save(any())).thenAnswer(invocation -> {
            OtpVerification saved = invocation.getArgument(0);
            saved.setId("otp-id");
            return saved;
        });
        org.mockito.Mockito.doThrow(new MessagingException("smtp unavailable"))
                .when(emailService).sendAuthenticationOtp(any(), any());

        assertThatThrownBy(() -> service.issue("active@example.com"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.AUTH_DELIVERY_FAILED));
        verify(mongoTemplate).updateFirst(any(), any(), org.mockito.ArgumentMatchers.eq(OtpVerification.class));
    }

    @Test
    void rejectsExpiredOtpAndPreventsReuse() {
        OtpVerification expired = verification("active@example.com", "123456");
        expired.setExpiresAt(NOW);
        when(otps.findTopByNormalizedEmailAndActiveTrueOrderByCreatedAtDesc("active@example.com"))
                .thenReturn(Optional.of(expired), Optional.empty());

        assertError(() -> service.verify("active@example.com", "123456"), ErrorCode.OTP_EXPIRED);
        assertError(() -> service.verify("active@example.com", "123456"), ErrorCode.INVALID_OTP);
    }

    @Test
    void rejectsOtpAtMaximumAttemptCount() {
        OtpVerification exhausted = verification("active@example.com", "123456");
        exhausted.setAttempts(5);
        when(otps.findTopByNormalizedEmailAndActiveTrueOrderByCreatedAtDesc("active@example.com"))
                .thenReturn(Optional.of(exhausted));

        assertError(() -> service.verify("active@example.com", "123456"),
                ErrorCode.OTP_ATTEMPTS_EXCEEDED);
    }

    @Test
    void consumesCorrectOtpExactlyOnce() {
        OtpVerification valid = verification("active@example.com", "123456");
        when(otps.findTopByNormalizedEmailAndActiveTrueOrderByCreatedAtDesc("active@example.com"))
                .thenReturn(Optional.of(valid), Optional.empty());
        when(mongoTemplate.findAndModify(any(), any(), any(FindAndModifyOptions.class),
                org.mockito.ArgumentMatchers.eq(OtpVerification.class))).thenReturn(valid);

        service.verify("active@example.com", "123456");
        assertError(() -> service.verify("active@example.com", "123456"), ErrorCode.INVALID_OTP);
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private OtpVerification verification(String email, String otp) {
        OtpVerification verification = new OtpVerification();
        verification.setId("otp-id");
        verification.setNormalizedEmail(email);
        verification.setOtpHash(hashService.hash(email, otp));
        verification.setExpiresAt(NOW.plusSeconds(300));
        verification.setCreatedAt(NOW);
        verification.setResendAllowedAt(NOW.plusSeconds(60));
        verification.setActive(true);
        return verification;
    }

    private User user(String email, UserStatus status) {
        User user = User.initialAdmin(email);
        user.setId("user-id");
        user.setStatus(status);
        return user;
    }
}
