package com.paywell.insights;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Money;
import com.paywell.insights.InsightsDtos.BillView;
import com.paywell.insights.InsightsDtos.BudgetView;
import com.paywell.insights.InsightsDtos.CreateBillRequest;
import com.paywell.insights.InsightsDtos.InsightsSummary;
import com.paywell.insights.InsightsDtos.MonthPoint;
import com.paywell.insights.InsightsDtos.TopCategory;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightsService {
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final RecurringBillRepository bills;
    private final Clock clock;
    private final ZoneId zone;

    public InsightsService(TransactionRepository transactions, BudgetRepository budgets, RecurringBillRepository bills, Clock clock, ZoneId userZone) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.bills = bills;
        this.clock = clock;
        this.zone = userZone;
    }

    @Transactional(readOnly = true)
    public InsightsSummary summary(UUID userId, int monthsBack) {
        int n = Math.min(Math.max(monthsBack, 1), 12);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        YearMonth current = YearMonth.from(today);
        YearMonth first = current.minusMonths(n - 1L);
        Instant since = first.atDay(1).atStartOfDay(zone).toInstant();
        List<Transaction> txs = transactions.findByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(userId, TxStatus.SUCCESS, since);

        Map<YearMonth, BigDecimal> income = new HashMap<>();
        Map<YearMonth, BigDecimal> spend = new HashMap<>();
        Map<Category, BigDecimal> byCategory = new HashMap<>();
        for (Transaction t : txs) {
            YearMonth ym = YearMonth.from(t.getCreatedAt().atZone(zone));
            if (t.getDirection() == Direction.DEBIT) {
                spend.merge(ym, t.getAmount(), BigDecimal::add);
                byCategory.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            } else if (t.getType() != TxType.TOPUP) {   // simulated top-ups are not income
                income.merge(ym, t.getAmount(), BigDecimal::add);
            }
        }
        List<MonthPoint> months = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            YearMonth ym = first.plusMonths(i);
            BigDecimal in = income.getOrDefault(ym, Money.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal out = spend.getOrDefault(ym, Money.ZERO).setScale(2, RoundingMode.HALF_UP);
            months.add(new MonthPoint(ym.toString(), in, out, in.subtract(out)));
        }
        BigDecimal totalSpend = Money.sum(byCategory.values());
        List<TopCategory> top = byCategory.entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed()).limit(5)
                .map(e -> new TopCategory(e.getKey().name(), e.getValue(), Money.percent(e.getValue(), totalSpend))).toList();

        MonthPoint cur = months.get(months.size() - 1);
        BigDecimal savings = cur.income().signum() == 0 ? null
                : cur.income().subtract(cur.spend()).multiply(BigDecimal.valueOf(100)).divide(cur.income(), 1, RoundingMode.HALF_UP);
        BigDecimal avgDaily = cur.spend().divide(BigDecimal.valueOf(Math.max(today.getDayOfMonth(), 1)), 2, RoundingMode.HALF_UP);
        BigDecimal change = null;
        if (months.size() >= 2) {
            BigDecimal prev = months.get(months.size() - 2).spend();
            if (prev.signum() > 0) {
                change = cur.spend().subtract(prev).multiply(BigDecimal.valueOf(100)).divide(prev, 1, RoundingMode.HALF_UP);
            }
        }
        return new InsightsSummary(months, top, savings, avgDaily, change,
                "Based on your demo activity. Simulated top-ups are excluded from income. Not financial advice.");
    }

    @Transactional(readOnly = true)
    public List<BudgetView> budgets(UUID userId) {
        return budgets.findByUserIdOrderByCategoryAsc(userId).stream().map(b -> new BudgetView(b.getCategory(), b.getMonthlyLimit())).toList();
    }

    @Transactional
    public BudgetView setBudget(UUID userId, String categoryRaw, BigDecimal limit) {
        Category c;
        try {
            c = Category.valueOf(categoryRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown category.");
        }
        if (c == Category.TOPUP || c == Category.REWARDS) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Budgets can only be set on spending categories.");
        }
        BigDecimal l = limit.setScale(Money.SCALE, RoundingMode.UNNECESSARY);
        Budget b = budgets.findByUserIdAndCategory(userId, c.name()).orElseGet(() -> new Budget(userId, c.name(), l));
        b.setMonthlyLimit(l);
        budgets.save(b);
        return new BudgetView(c.name(), l);
    }

    @Transactional(readOnly = true)
    public List<BillView> bills(UUID userId) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return bills.findByUserIdAndActiveTrueOrderByNextDueDateAsc(userId).stream()
                .map(b -> new BillView(b.getId(), b.getName(), b.getCategory(), b.getAmount(), b.getFrequency(), b.getNextDueDate(),
                        ChronoUnit.DAYS.between(today, b.getNextDueDate())))
                .sorted(Comparator.comparing(BillView::nextDueDate)).toList();
    }

    @Transactional
    public BillView addBill(UUID userId, CreateBillRequest r) {
        BigDecimal amount = Money.requirePositive(r.amount());
        RecurringBill b = bills.save(new RecurringBill(userId, r.name().trim(), r.category().trim().toUpperCase(), amount, r.frequency(), r.nextDueDate()));
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return new BillView(b.getId(), b.getName(), b.getCategory(), b.getAmount(), b.getFrequency(), b.getNextDueDate(),
                ChronoUnit.DAYS.between(today, b.getNextDueDate()));
    }

    @Transactional
    public void deleteBill(UUID userId, UUID id) {
        bills.delete(bills.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND)));
    }
}
