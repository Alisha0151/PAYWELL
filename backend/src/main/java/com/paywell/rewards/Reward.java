package com.paywell.rewards;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rewards")
public class Reward {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 16, updatable = false)
    private String kind;
    @Column(nullable = false, length = 120, updatable = false)
    private String title;
    @Column(length = 300, updatable = false)
    private String description;
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "source_transaction_id", updatable = false)
    private UUID sourceTransactionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "redeemed_at")
    private Instant redeemedAt;
    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    protected Reward() {}

    public Reward(UUID userId, String kind, String title, String description, BigDecimal amount, UUID sourceTransactionId,
                  Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.status = "AVAILABLE";
        this.sourceTransactionId = sourceTransactionId;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isRedeemable(Instant now) {
        return "AVAILABLE".equals(status) && (expiresAt == null || expiresAt.isAfter(now));
    }

    public void redeem(Instant now) {
        this.status = "REDEEMED";
        this.redeemedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
