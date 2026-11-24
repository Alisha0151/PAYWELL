package com.paywell.wallet;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Demo wallet. Balance is simulated funds, not a bank balance. Mutated only through WalletService under a row lock. */
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false, length = 24)
    private String kind = "DEMO_WALLET";
    @Version
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {}

    public Account(UUID id, UUID userId, String currency, Instant now) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.createdAt = now;
        this.updatedAt = now;
    }

    void credit(BigDecimal amount, Instant now) {
        this.balance = balance.add(amount);
        this.updatedAt = now;
    }

    void debit(BigDecimal amount, Instant now) {
        if (balance.compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        this.balance = balance.subtract(amount);
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public String getKind() { return kind; }
}
