package com.paywell.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** API representation. Amounts are serialised as decimal strings so no client ever round-trips money through a float. */
public record TransactionView(
        UUID id, String reference, TxType type, Direction direction, Category category,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String currency, TxStatus status,
        String counterpartyName, String counterpartyHandle, String note, boolean simulated, String failureReason,
        Instant createdAt, Instant completedAt) {

    public static TransactionView of(Transaction t) {
        return new TransactionView(t.getId(), t.getReference(), t.getType(), t.getDirection(), t.getCategory(), t.getAmount(),
                t.getCurrency(), t.getStatus(), t.getCounterpartyName(), t.getCounterpartyHandle(), t.getNote(), t.isSimulated(),
                t.getFailureReason(), t.getCreatedAt(), t.getCompletedAt());
    }
}
