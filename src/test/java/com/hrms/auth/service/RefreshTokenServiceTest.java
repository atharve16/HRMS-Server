package com.hrms.auth.service;

import com.hrms.auth.AuthTestProperties;
import com.hrms.auth.entity.RefreshToken;
import com.hrms.auth.repository.RefreshTokenRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    @Mock private RefreshTokenRepository repository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private SecureTokenGenerator generator;
    @Mock private TransactionTemplate transactions;
    private RefreshTokenService service;
    private TokenHashService hashService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hashService = new TokenHashService();
        service = new RefreshTokenService(repository, mongoTemplate, hashService, generator,
                AuthTestProperties.create(), Clock.fixed(NOW, ZoneOffset.UTC), transactions);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void rotatesRefreshTokenAndRevokesItsPredecessor() {
        RefreshToken current = token("raw-old", null);
        when(repository.findByTokenHash(hashService.hash("raw-old"))).thenReturn(Optional.of(current));
        when(generator.generateRefreshToken()).thenReturn("raw-new");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mongoTemplate.updateFirst(any(), any(), org.mockito.ArgumentMatchers.eq(RefreshToken.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        RefreshTokenService.RotatedRefreshToken rotated = service.rotate("raw-old");

        assertThat(rotated.userId()).isEqualTo("user-id");
        assertThat(rotated.issuedToken().rawToken()).isEqualTo("raw-new");
        assertThat(rotated.issuedToken().document().getTokenHash()).isEqualTo(hashService.hash("raw-new"));
        assertThat(rotated.issuedToken().document().getTokenFamilyId()).isEqualTo("family-id");
    }

    @Test
    void detectsRevokedTokenReuseAndRevokesTheFamily() {
        RefreshToken revoked = token("raw-old", NOW.minusSeconds(10));
        when(repository.findByTokenHash(hashService.hash("raw-old"))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotate("raw-old"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED));
        verify(mongoTemplate).updateMulti(any(), any(), org.mockito.ArgumentMatchers.eq(RefreshToken.class));
    }

    @Test
    void logoutRevokesTheWholeTokenFamily() {
        RefreshToken current = token("raw-old", null);
        when(repository.findByTokenHash(hashService.hash("raw-old"))).thenReturn(Optional.of(current));

        service.revokeByRawToken("raw-old");

        verify(mongoTemplate).updateMulti(any(), any(), org.mockito.ArgumentMatchers.eq(RefreshToken.class));
    }

    private RefreshToken token(String raw, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setId("token-id");
        token.setUserId("user-id");
        token.setTokenHash(hashService.hash(raw));
        token.setTokenFamilyId("family-id");
        token.setCreatedAt(NOW.minusSeconds(60));
        token.setExpiresAt(NOW.plusSeconds(3600));
        token.setRevokedAt(revokedAt);
        return token;
    }
}
