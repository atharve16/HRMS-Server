package com.hrms.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("otp_verifications")
@CompoundIndex(name = "otp_email_created_idx", def = "{'normalizedEmail': 1, 'createdAt': -1}")
@CompoundIndex(name = "one_active_otp_per_email", def = "{'normalizedEmail': 1}", unique = true,
        partialFilter = "{'active': true}")
public class OtpVerification {

    @Id
    private String id;

    @Indexed
    private String normalizedEmail;

    private String otpHash;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private int attempts;
    private Boolean active;
    private Instant consumedAt;
    private Instant createdAt;
    private Instant resendAllowedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResendAllowedAt() { return resendAllowedAt; }
    public void setResendAllowedAt(Instant resendAllowedAt) { this.resendAllowedAt = resendAllowedAt; }
}
