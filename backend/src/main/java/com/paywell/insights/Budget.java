package com.paywell.insights;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 24, updatable = false)
    private String category;
    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    protected Budget() {}

    public Budget(UUID userId, String category, BigDecimal monthlyLimit) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal v) { this.monthlyLimit = v; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCategory() { return category; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
}
