package com.paywell.common;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotency-Key handling for money-moving POSTs. The database enforces UNIQUE(user_id, idem_key); this service adds
 * the semantics: same key + same request => replay the stored result; same key + different request => 422.
 */
@Service
public class IdempotencyService {
    public static final String HEADER = "Idempotency-Key";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_\\-:.]{8,80}$");

    private final IdempotencyRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public String requireValid(String key) {
        if (key == null || key.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        if (!VALID.matcher(key).matches()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_INVALID, "Idempotency-Key must be 8-80 characters from A-Z a-z 0-9 _ - : .");
        }
        return key;
    }

    /** Stable SHA-256 over the request's meaningful fields, separated by an unprintable delimiter. */
    public static String hash(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            sb.append(p == null ? "" : p.toString()).append('\u001f');
        }
        return Hashing.sha256Hex(sb.toString());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UUID> findReplay(UUID userId, String key, String requestHash) {
        return repository.findByUserIdAndIdemKey(userId, key).map(existing -> {
            if (!Hashing.constantTimeEquals(existing.getRequestHash().trim(), requestHash)) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            return existing.getTransactionId();
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID userId, String key, String requestHash, UUID transactionId) {
        repository.save(new IdempotencyKey(userId, key, requestHash, transactionId, Instant.now(clock)));
    }
}
