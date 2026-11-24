package com.paywell.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 16, updatable = false)
    private String kind;
    @Column(nullable = false, length = 120, updatable = false)
    private String title;
    @Column(nullable = false, length = 300, updatable = false)
    private String body;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(UUID userId, String kind, String title, String body, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.createdAt = now;
    }

    public void markRead() { this.read = true; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
