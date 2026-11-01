package com.hrms.auth.controller;

import com.hrms.auth.dto.AuthenticatedUserResponse;
import com.hrms.auth.dto.AuthenticationResponse;
import com.hrms.auth.dto.LogoutResponse;
import com.hrms.auth.dto.OtpRequestResponse;
import com.hrms.auth.dto.RequestOtpRequest;
import com.hrms.auth.dto.VerifyOtpRequest;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.auth.service.AuthenticationService;
import com.hrms.auth.service.OtpRateLimiter;
import com.hrms.auth.service.RefreshCookieService;
import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import com.hrms.config.AuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Passwordless email OTP authentication")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final OtpRateLimiter rateLimiter;
    private final RefreshCookieService cookieService;
    private final AuthProperties properties;
    private final ClockSource clockSource;

    public AuthController(AuthenticationService authenticationService, OtpRateLimiter rateLimiter,
                          RefreshCookieService cookieService, AuthProperties properties,
                          ClockSource clockSource) {
        this.authenticationService = authenticationService;
        this.rateLimiter = rateLimiter;
        this.cookieService = cookieService;
        this.properties = properties;
        this.clockSource = clockSource;
    }

    @PostMapping("/request-otp")
    @Operation(summary = "Request an email sign-in code")
    public ApiResponse<OtpRequestResponse> requestOtp(@Valid @RequestBody RequestOtpRequest request,
                                                       HttpServletRequest servletRequest) {
        rateLimiter.check(servletRequest.getRemoteAddr());
        authenticationService.requestOtp(request.email());
        return ApiResponse.success("If the account is eligible, a sign-in code has been sent",
                new OtpRequestResponse(properties.otp().expiry().toSeconds()), clockSource);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify a sign-in code and start a session")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthenticationService.AuthResult result = authenticationService.verifyOtp(request.email(), request.otp());
        return tokenResponse("Authentication successful", result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(HttpServletRequest request) {
        AuthenticationService.AuthResult result = authenticationService.refresh(readRefreshCookie(request));
        return tokenResponse("Session refreshed", result);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke the current refresh-token family")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        authenticationService.logout(readRefreshCookie(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .body(ApiResponse.success("Logout successful", new LogoutResponse(true), clockSource));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Return the authenticated user's safe profile")
    public ApiResponse<AuthenticatedUserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Authenticated user retrieved",
                authenticationService.currentUser(principal), clockSource);
    }

    private ResponseEntity<ApiResponse<AuthenticationResponse>> tokenResponse(
            String message, AuthenticationService.AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(result.rawRefreshToken()).toString())
                .body(ApiResponse.success(message, result.response(), clockSource));
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieService.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
