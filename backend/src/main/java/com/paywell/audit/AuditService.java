package com.paywell.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Joins the caller's transaction so the audit row commits atomically with the change it describes. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID userId, String action, String entityType, String entityId, String detail) {
        repository.save(new AuditEvent(userId, userId == null ? "SYSTEM" : "USER:" + userId, action, entityType, entityId,
                truncate(detail), Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> recent(UUID userId, int page, int size) {
        return repository.findByUserIdOrderByIdDesc(userId, PageRequest.of(page, Math.min(size, 50)));
    }

    /** Detail must be short and non-sensitive: never pass passwords, tokens, OTPs or full personal data. */
    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 497) + "...";
    }
}
