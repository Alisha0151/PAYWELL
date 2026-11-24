package com.paywell.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "idem_key", nullable = false, updatable = false, length = 80)
    private String idemKey;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(UUID userId, String idemKey, String requestHash, UUID transactionId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.idemKey = idemKey;
        this.requestHash = requestHash;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public String getRequestHash() { return requestHash; }
    public UUID getTransactionId() { return transactionId; }
}
