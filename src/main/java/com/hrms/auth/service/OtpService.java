package com.hrms.auth.service;

import com.hrms.auth.entity.OtpVerification;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.OtpVerificationRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final MongoTemplate mongoTemplate;
    private final OtpHashService hashService;
    private final SecureTokenGenerator tokenGenerator;
    private final OtpEmailService emailService;
    private final AuthProperties properties;
    private final Clock clock;

    public OtpService(UserRepository userRepository, OtpVerificationRepository otpRepository,
                      MongoTemplate mongoTemplate, OtpHashService hashService,
                      SecureTokenGenerator tokenGenerator, OtpEmailService emailService,
                      AuthProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.mongoTemplate = mongoTemplate;
        this.hashService = hashService;
        this.tokenGenerator = tokenGenerator;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    public void issue(String normalizedEmail) {
        Instant now = clock.instant();
        Optional<User> user = userRepository.findByNormalizedEmail(normalizedEmail)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE);
        if (user.isEmpty()) {
            return;
        }

        Optional<OtpVerification> latest = otpRepository
                .findTopByNormalizedEmailOrderByCreatedAtDesc(normalizedEmail);
        if (latest.isPresent() && now.isBefore(latest.get().getResendAllowedAt())) {
            return;
        }

        String otp = tokenGenerator.generateOtp();
        OtpVerification verification = new OtpVerification();
        verification.setNormalizedEmail(normalizedEmail);
        verification.setOtpHash(hashService.hash(normalizedEmail, otp));
        verification.setExpiresAt(now.plus(properties.otp().expiry()));
        verification.setAttempts(0);
        verification.setCreatedAt(now);
        verification.setResendAllowedAt(now.plus(properties.otp().resendCooldown()));
        // Reserve the code while it is being delivered. It becomes usable only after SMTP succeeds.
        verification.setActive(false);
        verification.setConsumedAt(now);
        try {
            verification = otpRepository.save(verification);
            emailService.sendAuthenticationOtp(user.get().getEmail(), otp);
            consumeActive(normalizedEmail, now);
            activate(verification.getId());
        } catch (MessagingException | java.io.UnsupportedEncodingException | MailException exception) {
            if (verification.getId() != null) {
                consume(verification.getId(), now);
            }
            log.warn("Authentication email delivery failed for user {}", user.get().getId());
            throw new ApiException(ErrorCode.AUTH_DELIVERY_FAILED, HttpStatus.SERVICE_UNAVAILABLE,
                    "The authentication email could not be delivered; please try again later");
        } catch (DuplicateKeyException exception) {
            log.info("Concurrent OTP issuance was suppressed for user {}", user.get().getId());
        }
    }

    public OtpVerification verify(String normalizedEmail, String otp) {
        Instant now = clock.instant();
        OtpVerification verification = otpRepository
                .findTopByNormalizedEmailAndActiveTrueOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(this::invalidOtp);
        if (!now.isBefore(verification.getExpiresAt())) {
            consume(verification.getId(), now);
            throw new ApiException(ErrorCode.OTP_EXPIRED, HttpStatus.UNAUTHORIZED,
                    "The one-time code has expired");
        }
        if (verification.getAttempts() >= properties.otp().maxAttempts()) {
            consume(verification.getId(), now);
            throw attemptsExceeded();
        }
        if (!hashService.matches(normalizedEmail, otp, verification.getOtpHash())) {
            OtpVerification updated = mongoTemplate.findAndModify(
                    activeQuery(verification.getId()).addCriteria(
                            Criteria.where("attempts").lt(properties.otp().maxAttempts())),
                    new Update().inc("attempts", 1),
                    FindAndModifyOptions.options().returnNew(true), OtpVerification.class);
            if (updated == null || updated.getAttempts() >= properties.otp().maxAttempts()) {
                consume(verification.getId(), now);
                throw attemptsExceeded();
            }
            throw invalidOtp();
        }

        OtpVerification consumed = mongoTemplate.findAndModify(
                activeQuery(verification.getId())
                        .addCriteria(Criteria.where("expiresAt").gt(now))
                        .addCriteria(Criteria.where("attempts").lt(properties.otp().maxAttempts())),
                new Update().set("active", false).set("consumedAt", now),
                FindAndModifyOptions.options().returnNew(true), OtpVerification.class);
        if (consumed == null) {
            throw invalidOtp();
        }
        return consumed;
    }

    private void consumeActive(String normalizedEmail, Instant now) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("normalizedEmail").is(normalizedEmail)
                        .and("active").is(true)),
                new Update().set("active", false).set("consumedAt", now), OtpVerification.class);
    }

    private void consume(String id, Instant now) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update().set("active", false).set("consumedAt", now), OtpVerification.class);
    }

    private void activate(String id) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id).and("active").is(false)),
                new Update().set("active", true).unset("consumedAt"), OtpVerification.class);
    }

    private Query activeQuery(String id) {
        return Query.query(Criteria.where("_id").is(id).and("active").is(true));
    }

    private ApiException invalidOtp() {
        return new ApiException(ErrorCode.INVALID_OTP, HttpStatus.UNAUTHORIZED,
                "The one-time code is invalid");
    }

    private ApiException attemptsExceeded() {
        return new ApiException(ErrorCode.OTP_ATTEMPTS_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS,
                "The maximum number of verification attempts has been reached");
    }
}
