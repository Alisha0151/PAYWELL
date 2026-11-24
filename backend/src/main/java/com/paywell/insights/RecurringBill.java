package com.paywell.insights;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A bill the user declared (reminder only). PAYWELL does not auto-pay or pull funds. */
@Entity
@Table(name = "recurring_bills")
public class RecurringBill {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, length = 24)
    private String category;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 8)
    private String frequency;
    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;
    @Column(nullable = false)
    private boolean active = true;

    protected RecurringBill() {}

    public RecurringBill(UUID userId, String name, String category, BigDecimal amount, String frequency, LocalDate nextDueDate) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.frequency = frequency;
        this.nextDueDate = nextDueDate;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getFrequency() { return frequency; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public boolean isActive() { return active; }
}
