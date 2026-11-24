package com.paywell.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A "request money" record: the RECEIVE side of the flow. */
@Entity
@Table(name = "payment_requests")
public class PaymentRequestEntity {
    @Id
    private UUID id;
    @Column(name = "requester_user_id", nullable = false, updatable = false)
    private UUID requesterUserId;
    @Column(name = "payer_handle", nullable = false, length = 60, updatable = false)
    private String payerHandle;
    @Column(name = "payer_user_id", updatable = false)
    private UUID payerUserId;
    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Column(length = 140, updatable = false)
    private String note;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "paid_transaction_id")
    private UUID paidTransactionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected PaymentRequestEntity() {}

    public PaymentRequestEntity(UUID requesterUserId, String payerHandle, UUID payerUserId, BigDecimal amount, String currency,
                                String note, Instant now) {
        this.id = UUID.randomUUID();
        this.requesterUserId = requesterUserId;
        this.payerHandle = payerHandle;
        this.payerUserId = payerUserId;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
        this.status = "PENDING";
        this.createdAt = now;
    }

    public boolean isPending() { return "PENDING".equals(status); }

    public void markPaid(UUID transactionId, Instant now) {
        this.status = "PAID";
        this.paidTransactionId = transactionId;
        this.resolvedAt = now;
    }

    public void markDeclined(Instant now) {
        this.status = "DECLINED";
        this.resolvedAt = now;
    }

    public void markCancelled(Instant now) {
        this.status = "CANCELLED";
        this.resolvedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public String getPayerHandle() { return payerHandle; }
    public UUID getPayerUserId() { return payerUserId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public UUID getPaidTransactionId() { return paidTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
