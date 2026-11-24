package com.paywell.radar;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RadarDtos {
    private RadarDtos() {}

    private static final String S = "STRING";

    public record CategoryShare(String category, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, int count,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal sharePercent,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal previousAmount,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal changePercent) {}

    public record Anomaly(UUID transactionId, String reference, String kind, String severity, String message,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, Instant at) {}

    public record Subscription(String name, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String frequency,
                               Instant lastCharged, LocalDate nextExpected, int occurrences) {}

    public record BudgetStatus(String category, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal limit,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spent,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal remaining,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal percentUsed, String state) {}

    public record RadarResponse(Instant from, Instant to,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalSpent,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalReceived,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal net,
                                List<CategoryShare> categories, List<Anomaly> anomalies, List<Subscription> subscriptions,
                                List<BudgetStatus> budgets, String notice) {}
}
