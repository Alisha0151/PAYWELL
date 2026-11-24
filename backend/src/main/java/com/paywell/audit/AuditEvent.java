package com.paywell.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Append-only audit record. No setters; Hibernate never issues UPDATEs; a database trigger also forbids them. */
@Entity
@Immutable
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", updatable = false)
    private UUID userId;
    @Column(nullable = false, updatable = false, length = 64)
    private String actor;
    @Column(nullable = false, updatable = false, length = 60)
    private String action;
    @Column(name = "entity_type", updatable = false, length = 40)
    private String entityType;
    @Column(name = "entity_id", updatable = false, length = 64)
    private String entityId;
    @Column(updatable = false, length = 500)
    private String detail;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {}

    AuditEvent(UUID userId, String actor, String action, String entityType, String entityId, String detail, Instant createdAt) {
        this.userId = userId;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
