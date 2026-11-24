package com.paywell.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record ProfileView(UUID id, String email, String fullName, String phone, String handle, String currency, String language,
                              boolean notifyPayments, boolean notifyRewards, boolean notifyBills, boolean notifySecurity,
                              boolean hideBalanceDefault, boolean shareAnalytics, Instant memberSince) {}

    /** All fields optional (PATCH semantics). */
    public record UpdateProfileRequest(
            @Size(min = 2, max = 120) String fullName,
            @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "phone must be 7-15 digits, optionally starting with +") String phone,
            @Pattern(regexp = "^(en|hi)$", message = "language must be en or hi") String language,
            Boolean notifyPayments, Boolean notifyRewards, Boolean notifyBills, Boolean notifySecurity,
            Boolean hideBalanceDefault, Boolean shareAnalytics) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 10, max = 72, message = com.paywell.auth.AuthDtos.PASSWORD_MESSAGE)
            @Pattern(regexp = com.paywell.auth.AuthDtos.PASSWORD_REGEX, message = com.paywell.auth.AuthDtos.PASSWORD_MESSAGE) String newPassword) {}

    public record PaymentMethodView(UUID id, String kind, String label, String last4, String status, boolean isDefault) {}

    /** Metadata only. Full card or bank account numbers must never be sent to or stored by this API. */
    public record AddPaymentMethodRequest(
            @NotBlank @Pattern(regexp = "^(BANK_ACCOUNT|CARD)$", message = "kind must be BANK_ACCOUNT or CARD") String kind,
            @NotBlank @Size(max = 60) String label,
            @Pattern(regexp = "^[0-9]{4}$", message = "last4 must be exactly 4 digits") String last4) {}

    public record ActivityView(Long id, String action, String entityType, String detail, Instant at) {}
}
