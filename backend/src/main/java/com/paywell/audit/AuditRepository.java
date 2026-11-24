package com.paywell.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/** Deliberately NOT a CrudRepository: only insert and read operations are exposed. */
public interface AuditRepository extends Repository<AuditEvent, Long> {
    AuditEvent save(AuditEvent event);

    Page<AuditEvent> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);
}
