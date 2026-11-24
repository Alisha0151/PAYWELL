package com.paywell.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A refresh-token session. Only the SHA-256 of the opaque token is stored. Rotation + reuse detection by family. */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "refresh_token_hash", nullable = false, length = 64, updatable = false)
    private String refreshTokenHash;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "rotated_at")
    private Instant rotatedAt;
    @Column(name = "user_agent", length = 200, updatable = false)
    private String userAgent;

    protected AuthSession() {}

    public AuthSession(UUID userId, UUID familyId, String refreshTokenHash, Instant now, Instant expiresAt, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.refreshTokenHash = refreshTokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent == null ? null : (userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && rotatedAt == null && expiresAt.isAfter(now);
    }

    public void markRotated(Instant now) { this.rotatedAt = now; }

    public void revoke(Instant now) { if (revokedAt == null) this.revokedAt = now; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public Instant getRotatedAt() { return rotatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
