package com.paywell.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** One row per provider attempt; insert-only history. */
@Entity
@Immutable
@Table(name = "payment_attempts")
public class PaymentAttempt {
    @Id
    private UUID id;
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;
    @Column(nullable = false, length = 32, updatable = false)
    private String provider;
    @Column(name = "provider_reference", length = 64, updatable = false)
    private String providerReference;
    @Column(nullable = false, length = 16, updatable = false)
    private String outcome;
    @Column(name = "failure_code", length = 60, updatable = false)
    private String failureCode;
    @Column(name = "risk_score", updatable = false)
    private Integer riskScore;
    @Column(name = "risk_category", length = 8, updatable = false)
    private String riskCategory;
    @Column(name = "risk_action", length = 12, updatable = false)
    private String riskAction;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentAttempt() {}

    public PaymentAttempt(UUID transactionId, String provider, String providerReference, String outcome, String failureCode,
                          Integer riskScore, String riskCategory, String riskAction, Instant now) {
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.provider = provider;
        this.providerReference = providerReference;
        this.outcome = outcome;
        this.failureCode = failureCode;
        this.riskScore = riskScore;
        this.riskCategory = riskCategory;
        this.riskAction = riskAction;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public String getOutcome() { return outcome; }
}
