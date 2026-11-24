package com.paywell.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipients")
public class Recipient {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 60, updatable = false)
    private String handle;
    @Column(nullable = false)
    private boolean favorite;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Recipient() {}

    public Recipient(UUID userId, String name, String handle, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.handle = handle;
        this.createdAt = now;
    }

    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getHandle() { return handle; }
    public boolean isFavorite() { return favorite; }
    public Instant getCreatedAt() { return createdAt; }
}
