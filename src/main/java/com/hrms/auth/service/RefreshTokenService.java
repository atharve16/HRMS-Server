package com.hrms.auth.service;

import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.repository.RefreshTokenRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.config.AuthProperties;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final MongoTemplate mongoTemplate;
    private final TokenHashService hashService;
    private final SecureTokenGenerator tokenGenerator;
    private final AuthProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RefreshTokenService(RefreshTokenRepository repository, MongoTemplate mongoTemplate,
                               TokenHashService hashService, SecureTokenGenerator tokenGenerator,
                               AuthProperties properties, Clock clock, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.hashService = hashService;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public IssuedRefreshToken issue(String userId) {
        return create(userId, UUID.randomUUID().toString());
    }

    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(hashService.hash(rawToken))
                .orElseThrow(this::invalidToken);
        if (current.getRevokedAt() != null) {
            revokeFamily(current.getTokenFamilyId(), now);
            throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED, HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse was detected; the session has been revoked");
        }
        if (!now.isBefore(current.getExpiresAt())) {
            revokeFamily(current.getTokenFamilyId(), now);
            throw invalidToken();
        }

        try {
            return transactionTemplate.execute(status -> {
                IssuedRefreshToken replacement = createUnsaved(current.getUserId(), current.getTokenFamilyId());
                repository.save(replacement.document());
                var result = mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(current.getId()).and("revokedAt").is(null)),
                        new Update().set("revokedAt", now)
                                .set("replacedByTokenId", replacement.document().getId()),
                        RefreshToken.class);
                if (result.getModifiedCount() != 1) {
                    status.setRollbackOnly();
                    throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED, HttpStatus.UNAUTHORIZED,
                            "Refresh token reuse was detected; the session has been revoked");
                }
                return new RotatedRefreshToken(current.getUserId(), replacement);
            });
        } catch (ApiException exception) {
            if (exception.getErrorCode() == ErrorCode.REFRESH_TOKEN_REUSED) {
                revokeFamily(current.getTokenFamilyId(), now);
            }
            throw exception;
        }
    }

    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(hashService.hash(rawToken))
                .ifPresent(token -> revokeFamily(token.getTokenFamilyId(), clock.instant()));
    }

    private IssuedRefreshToken create(String userId, String familyId) {
        IssuedRefreshToken issued = createUnsaved(userId, familyId);
        repository.save(issued.document());
        return issued;
    }

    private IssuedRefreshToken createUnsaved(String userId, String familyId) {
        Instant now = clock.instant();
        String rawToken = tokenGenerator.generateRefreshToken();
        RefreshToken document = new RefreshToken();
        document.setId(new ObjectId().toHexString());
        document.setUserId(userId);
        document.setTokenHash(hashService.hash(rawToken));
        document.setTokenFamilyId(familyId);
        document.setCreatedAt(now);
        document.setExpiresAt(now.plus(properties.jwt().refreshExpiry()));
        return new IssuedRefreshToken(rawToken, document);
    }

    private void revokeFamily(String familyId, Instant now) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("tokenFamilyId").is(familyId).and("revokedAt").is(null)),
                new Update().set("revokedAt", now), RefreshToken.class);
    }

    private ApiException invalidToken() {
        return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED,
                "The refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken document) {
    }

    public record RotatedRefreshToken(String userId, IssuedRefreshToken issuedToken) {
    }
}
