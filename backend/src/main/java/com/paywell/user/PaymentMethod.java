package com.paywell.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Metadata only (label + last 4). Raw card or account numbers are never accepted or stored. */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 16)
    private String kind;
    @Column(nullable = false, length = 60)
    private String label;
    @Column(length = 4)
    private String last4;
    @Column(nullable = false, length = 16)
    private String status = "UNVERIFIED";
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentMethod() {}

    public PaymentMethod(UUID id, UUID userId, String kind, String label, String last4, boolean isDefault, Instant now) {
        this.id = id;
        this.userId = userId;
        this.kind = kind;
        this.label = label;
        this.last4 = last4;
        this.isDefault = isDefault;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getKind() { return kind; }
    public String getLabel() { return label; }
    public String getLast4() { return last4; }
    public String getStatus() { return status; }
    public boolean isDefault() { return isDefault; }
}
