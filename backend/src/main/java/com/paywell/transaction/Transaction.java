package com.paywell.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 32, updatable = false)
    private String reference;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8, updatable = false)
    private Direction direction;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24, updatable = false)
    private TxType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Category category;
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxStatus status;
    @Column(name = "counterparty_name", length = 120)
    private String counterpartyName;
    @Column(name = "counterparty_handle", length = 60)
    private String counterpartyHandle;
    @Column(name = "recipient_id")
    private UUID recipientId;
    @Column(length = 140)
    private String note;
    @Column(nullable = false)
    private boolean simulated = true;
    @Column(name = "failure_reason", length = 60)
    private String failureReason;
    @Column(name = "reverses_transaction_id")
    private UUID reversesTransactionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transaction() {}

    public Transaction(UUID id, String reference, UUID userId, UUID accountId, Direction direction, TxType type,
                       Category category, BigDecimal amount, String currency, TxStatus status, String counterpartyName,
                       String counterpartyHandle, UUID recipientId, String note, Instant createdAt) {
        this.id = id;
        this.reference = reference;
        this.userId = userId;
        this.accountId = accountId;
        this.direction = direction;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.counterpartyName = counterpartyName;
        this.counterpartyHandle = counterpartyHandle;
        this.recipientId = recipientId;
        this.note = note;
        this.createdAt = createdAt;
    }

    public void markSuccess(Instant now) {
        this.status = TxStatus.SUCCESS;
        this.completedAt = now;
        this.failureReason = null;
    }

    public void markFailed(String reason, Instant now) {
        this.status = TxStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = now;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public UUID getUserId() { return userId; }
    public UUID getAccountId() { return accountId; }
    public Direction getDirection() { return direction; }
    public TxType getType() { return type; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TxStatus getStatus() { return status; }
    public String getCounterpartyName() { return counterpartyName; }
    public String getCounterpartyHandle() { return counterpartyHandle; }
    public UUID getRecipientId() { return recipientId; }
    public String getNote() { return note; }
    public boolean isSimulated() { return simulated; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
