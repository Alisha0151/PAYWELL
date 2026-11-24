package com.paywell.insights;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InsightsDtos {
    private InsightsDtos() {}

    public record MonthPoint(String month, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal income,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spend,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal net) {}

    public record TopCategory(String category, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal sharePercent) {}

    public record InsightsSummary(List<MonthPoint> months, List<TopCategory> topCategories,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal savingsRatePercent,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal averageDailySpendThisMonth,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal spendChangeVsLastMonthPercent, String notice) {}

    public record SetBudgetRequest(@NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal monthlyLimit) {}

    public record BudgetView(String category, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monthlyLimit) {}

    public record CreateBillRequest(@NotBlank @Size(max = 80) String name,
                                    @NotBlank @Size(max = 24) String category,
                                    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                                    @NotBlank @Pattern(regexp = "^(WEEKLY|MONTHLY|YEARLY)$", message = "frequency must be WEEKLY, MONTHLY or YEARLY") String frequency,
                                    @NotNull @FutureOrPresent LocalDate nextDueDate) {}

    public record BillView(UUID id, String name, String category, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
                           String frequency, LocalDate nextDueDate, long daysUntilDue) {}
}
