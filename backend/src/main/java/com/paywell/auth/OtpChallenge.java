package com.paywell.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 24, updatable = false)
    private String purpose;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code_hash", nullable = false, length = 64, updatable = false)
    private String codeHash;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected OtpChallenge() {}

    public OtpChallenge(UUID userId, String purpose, String codeHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(now) && attempts < maxAttempts;
    }

    public void registerAttempt() { attempts++; }

    public void consume(Instant now) { this.consumedAt = now; }

    public UUID getId() { return id; }
    public String getCodeHash() { return codeHash; }
    public int getAttempts() { return attempts; }
}
