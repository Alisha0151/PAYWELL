package com.paywell.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(nullable = false, length = 40)
    private String handle;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "INR";
    @Column(nullable = false, length = 8)
    private String language = "en";
    @Column(name = "notify_payments", nullable = false)
    private boolean notifyPayments = true;
    @Column(name = "notify_rewards", nullable = false)
    private boolean notifyRewards = true;
    @Column(name = "notify_bills", nullable = false)
    private boolean notifyBills = true;
    @Column(name = "notify_security", nullable = false)
    private boolean notifySecurity = true;
    @Column(name = "hide_balance_default", nullable = false)
    private boolean hideBalanceDefault;
    @Column(name = "share_analytics", nullable = false)
    private boolean shareAnalytics;

    protected UserProfile() {}

    public UserProfile(UUID userId, String handle, String currency) {
        this.userId = userId;
        this.handle = handle;
        this.currency = currency;
    }

    public void update(String language, Boolean notifyPayments, Boolean notifyRewards, Boolean notifyBills,
                       Boolean notifySecurity, Boolean hideBalanceDefault, Boolean shareAnalytics) {
        if (language != null) this.language = language;
        if (notifyPayments != null) this.notifyPayments = notifyPayments;
        if (notifyRewards != null) this.notifyRewards = notifyRewards;
        if (notifyBills != null) this.notifyBills = notifyBills;
        if (notifySecurity != null) this.notifySecurity = notifySecurity;
        if (hideBalanceDefault != null) this.hideBalanceDefault = hideBalanceDefault;
        if (shareAnalytics != null) this.shareAnalytics = shareAnalytics;
    }

    public UUID getUserId() { return userId; }
    public String getHandle() { return handle; }
    public String getCurrency() { return currency; }
    public String getLanguage() { return language; }
    public boolean isNotifyPayments() { return notifyPayments; }
    public boolean isNotifyRewards() { return notifyRewards; }
    public boolean isNotifyBills() { return notifyBills; }
    public boolean isNotifySecurity() { return notifySecurity; }
    public boolean isHideBalanceDefault() { return hideBalanceDefault; }
    public boolean isShareAnalytics() { return shareAnalytics; }
}
