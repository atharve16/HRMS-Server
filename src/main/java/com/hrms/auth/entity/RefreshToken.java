package com.hrms.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("refresh_tokens")
@CompoundIndex(name = "refresh_family_revoked_idx", def = "{'tokenFamilyId': 1, 'revokedAt': 1}")
public class RefreshToken {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String tokenFamilyId;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private Instant revokedAt;
    private Instant createdAt;
    private String replacedByTokenId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getTokenFamilyId() { return tokenFamilyId; }
    public void setTokenFamilyId(String tokenFamilyId) { this.tokenFamilyId = tokenFamilyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getReplacedByTokenId() { return replacedByTokenId; }
    public void setReplacedByTokenId(String replacedByTokenId) { this.replacedByTokenId = replacedByTokenId; }
}
