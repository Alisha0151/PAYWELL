package com.paywell.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paywell.transaction.TransactionView;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {}

    public static final String HANDLE_REGEX = "^[A-Za-z0-9._@-]{3,60}$";

    /** Provide either recipientId (a saved recipient) or handle. acknowledgeRisk confirms a CHALLENGE/REVIEW step-up in demo mode. */
    public record SendPaymentRequest(
            UUID recipientId,
            @Pattern(regexp = HANDLE_REGEX, message = "handle must be 3-60 characters: letters, digits . _ @ -") String handle,
            @Size(max = 120) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 140) String note,
            @Size(max = 24) String category,
            Boolean acknowledgeRisk) {}

    public record RequestMoneyRequest(
            @NotNull @Pattern(regexp = HANDLE_REGEX, message = "handle must be 3-60 characters: letters, digits . _ @ -") String payerHandle,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 140) String note) {}

    public record PaymentResponse(TransactionView transaction, boolean replayed, RiskAssessment risk, String notice) {
        public static final String NOTICE = "SIMULATED payment: no real money moved.";
    }

    public record PaymentRequestView(
            UUID id, String direction, String counterpartyHandle,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String currency, String note, String status,
            Instant createdAt, Instant resolvedAt, UUID paidTransactionId) {}

    public record RecipientView(UUID id, String name, String handle, boolean favorite) {}

    public record CreateRecipientRequest(
            @NotNull @Size(min = 1, max = 120) String name,
            @NotNull @Pattern(regexp = HANDLE_REGEX, message = "handle must be 3-60 characters: letters, digits . _ @ -") String handle) {}

    public record FavoriteRequest(@NotNull Boolean favorite) {}
}
