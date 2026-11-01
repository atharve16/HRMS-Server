package com.hrms.auth.service;

import com.hrms.auth.dto.AuthenticatedUserResponse;
import com.hrms.auth.dto.AuthenticationResponse;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.security.JwtService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.config.AuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final EmailNormalizer emailNormalizer;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthenticationService(UserRepository userRepository, OtpService otpService,
                                 RefreshTokenService refreshTokenService, JwtService jwtService,
                                 EmailNormalizer emailNormalizer, AuthProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.emailNormalizer = emailNormalizer;
        this.properties = properties;
        this.clock = clock;
    }

    public void requestOtp(String email) {
        otpService.issue(emailNormalizer.normalize(email));
    }

    public AuthResult verifyOtp(String email, String otp) {
        String normalizedEmail = emailNormalizer.normalize(email);
        User user = activeUserByEmail(normalizedEmail);
        otpService.verify(normalizedEmail, otp);
        user.setLastLoginAt(clock.instant());
        userRepository.save(user);
        return tokensFor(user, refreshTokenService.issue(user.getId()));
    }

    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotated.userId()).orElseThrow(this::invalidRefreshToken);
        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeByRawToken(rotated.issuedToken().rawToken());
            throw new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.UNAUTHORIZED,
                    "The account is inactive");
        }
        return tokensFor(user, rotated.issuedToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    public AuthenticatedUserResponse currentUser(AuthPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                        "Authentication is required"));
        return AuthenticatedUserResponse.from(user);
    }

    private User activeUserByEmail(String normalizedEmail) {
        User user = userRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OTP, HttpStatus.UNAUTHORIZED,
                        "The one-time code is invalid"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.EMPLOYEE_INACTIVE, HttpStatus.UNAUTHORIZED,
                    "The account is inactive");
        }
        return user;
    }

    private AuthResult tokensFor(User user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        AuthenticationResponse response = new AuthenticationResponse(
                jwtService.issueAccessToken(user), "Bearer", properties.jwt().accessExpiry().toSeconds(),
                AuthenticatedUserResponse.from(user));
        return new AuthResult(response, refreshToken.rawToken());
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED,
                "The refresh token is invalid or expired");
    }

    public record AuthResult(AuthenticationResponse response, String rawRefreshToken) {
    }
}
