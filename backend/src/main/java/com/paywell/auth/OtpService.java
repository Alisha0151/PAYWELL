package com.paywell.auth;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Hashing;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    static final Duration TTL = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;

    private final OtpChallengeRepository repository;
    private final OtpSender sender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpChallengeRepository repository, OtpSender sender, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.clock = clock;
    }

    public boolean deliveryConfigured() { return sender.deliveryConfigured(); }

    /** Creates a fresh single-use code (older open codes are invalidated) and hands it to the sender. Returns whether it was delivered. */
    @Transactional
    public boolean issue(UUID userId, String destination, String purpose) {
        Instant now = Instant.now(clock);
        repository.invalidateOpen(userId, purpose, now);
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpChallenge c = new OtpChallenge(userId, purpose, hash(userId, purpose, code), now, now.plus(TTL));
        repository.save(c);
        return sender.send(destination, purpose, code);
    }

    /**
     * Verifies and consumes a code. Failed attempts are persisted (noRollbackFor) so the attempt limit cannot be bypassed.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyAndConsume(UUID userId, String purpose, String code) {
        Instant now = Instant.now(clock);
        OtpChallenge c = repository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, purpose)
                .filter(x -> x.isUsable(now, MAX_ATTEMPTS))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_OTP));
        c.registerAttempt();
        if (code == null || !Hashing.constantTimeEquals(c.getCodeHash().trim(), hash(userId, purpose, code))) {
            throw new ApiException(ErrorCode.INVALID_OTP);
        }
        c.consume(now);
    }

    static String hash(UUID userId, String purpose, String code) {
        return Hashing.sha256Hex(userId + ":" + purpose + ":" + code);
    }
}
