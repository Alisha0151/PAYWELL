package com.paywell.radar;

import com.paywell.common.Money;
import com.paywell.insights.Budget;
import com.paywell.insights.BudgetRepository;
import com.paywell.radar.RadarDtos.Anomaly;
import com.paywell.radar.RadarDtos.BudgetStatus;
import com.paywell.radar.RadarDtos.CategoryShare;
import com.paywell.radar.RadarDtos.RadarResponse;
import com.paywell.radar.RadarDtos.Subscription;
import com.paywell.transaction.Direction;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Money Radar: transparent heuristics over the user's own (demo) transactions. These are rules of thumb, not predictions,
 * and nothing here is financial advice.
 */
@Service
public class MoneyRadarService {
    static final BigDecimal LARGE_MULTIPLE = new BigDecimal("5");
    static final BigDecimal VERY_LARGE_MULTIPLE = new BigDecimal("10");
    static final BigDecimal LARGE_FLOOR = new BigDecimal("1000");
    static final BigDecimal NEW_COUNTERPARTY_FLOOR = new BigDecimal("5000");
    static final BigDecimal LATE_NIGHT_FLOOR = new BigDecimal("2000");

    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final Clock clock;
    private final ZoneId zone;

    public MoneyRadarService(TransactionRepository transactions, BudgetRepository budgets, Clock clock, ZoneId userZone) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.clock = clock;
        this.zone = userZone;
    }

    @Transactional(readOnly = true)
    public RadarResponse radar(UUID userId) {
        Instant now = Instant.now(clock);
        Instant from = now.minus(Duration.ofDays(30));
        Instant prevFrom = now.minus(Duration.ofDays(60));
        List<Transaction> all = transactions.findByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                userId, TxStatus.SUCCESS, now.minus(Duration.ofDays(90)));
        List<Transaction> debits = all.stream().filter(t -> t.getDirection() == Direction.DEBIT).toList();
        List<Transaction> period = debits.stream().filter(t -> !t.getCreatedAt().isBefore(from)).toList();
        List<Transaction> previous = debits.stream().filter(t -> !t.getCreatedAt().isBefore(prevFrom) && t.getCreatedAt().isBefore(from)).toList();

        BigDecimal spent = Money.sum(period.stream().map(Transaction::getAmount).toList());
        BigDecimal received = Money.sum(all.stream().filter(t -> t.getDirection() == Direction.CREDIT && t.getType() != TxType.TOPUP
                && !t.getCreatedAt().isBefore(from)).map(Transaction::getAmount).toList());

        return new RadarResponse(from, now, spent, received, received.subtract(spent), categories(period, previous, spent),
                anomalies(debits, from), subscriptions(debits), budgetStatuses(userId, debits, now),
                "Heuristic insights from your demo activity. Not financial advice and not a guarantee of fraud detection.");
    }

    List<CategoryShare> categories(List<Transaction> period, List<Transaction> previous, BigDecimal total) {
        Map<String, List<Transaction>> byCat = new LinkedHashMap<>();
        period.forEach(t -> byCat.computeIfAbsent(t.getCategory().name(), k -> new ArrayList<>()).add(t));
        Map<String, BigDecimal> prev = new HashMap<>();
        previous.forEach(t -> prev.merge(t.getCategory().name(), t.getAmount(), BigDecimal::add));
        List<CategoryShare> out = new ArrayList<>();
        byCat.forEach((cat, list) -> {
            BigDecimal amt = Money.sum(list.stream().map(Transaction::getAmount).toList());
            BigDecimal p = prev.getOrDefault(cat, Money.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal change = p.signum() == 0 ? null
                    : amt.subtract(p).multiply(BigDecimal.valueOf(100)).divide(p, 1, RoundingMode.HALF_UP);
            out.add(new CategoryShare(cat, amt, list.size(), Money.percent(amt, total), p, change));
        });
        out.sort(Comparator.comparing(CategoryShare::amount).reversed());
        return out;
    }

    List<Anomaly> anomalies(List<Transaction> debits90d, Instant from) {
        List<BigDecimal> amounts = debits90d.stream().map(Transaction::getAmount).toList();
        BigDecimal median = amounts.size() >= 5 ? Stats.median(amounts) : null;
        Map<UUID, Anomaly> found = new LinkedHashMap<>();
        Map<String, Instant> firstSeen = new HashMap<>();
        Transaction prev = null;
        for (Transaction t : debits90d) {
            String who = t.getCounterpartyName() == null ? "" : t.getCounterpartyName().toLowerCase(Locale.ROOT);
            boolean firstTime = !who.isEmpty() && firstSeen.putIfAbsent(who, t.getCreatedAt()) == null;
            boolean inPeriod = !t.getCreatedAt().isBefore(from);
            if (inPeriod) {
                if (median != null && median.signum() > 0 && t.getAmount().compareTo(LARGE_FLOOR) >= 0
                        && t.getAmount().compareTo(median.multiply(LARGE_MULTIPLE)) >= 0) {
                    String sev = t.getAmount().compareTo(median.multiply(VERY_LARGE_MULTIPLE)) >= 0 ? "HIGH" : "MEDIUM";
                    put(found, t, "LARGE_VS_TYPICAL", sev, "This payment is much larger than your typical payment (median " + median.toPlainString() + ").");
                }
                if (firstTime && t.getAmount().compareTo(NEW_COUNTERPARTY_FLOOR) >= 0) {
                    put(found, t, "NEW_COUNTERPARTY_LARGE", "MEDIUM", "Large first payment to a new counterparty.");
                }
                if (prev != null && prev.getCounterpartyName() != null && prev.getCounterpartyName().equalsIgnoreCase(t.getCounterpartyName())
                        && prev.getAmount().compareTo(t.getAmount()) == 0 && Duration.between(prev.getCreatedAt(), t.getCreatedAt()).toMinutes() <= 10) {
                    put(found, t, "POSSIBLE_DUPLICATE", "MEDIUM", "Same amount to the same counterparty within 10 minutes.");
                }
                int hour = t.getCreatedAt().atZone(zone).getHour();
                if (hour < 5 && t.getAmount().compareTo(LATE_NIGHT_FLOOR) >= 0) {
                    put(found, t, "LATE_NIGHT", "LOW", "Sizeable payment made late at night.");
                }
            }
            prev = t;
        }
        return found.values().stream()
                .sorted(Comparator.comparing((Anomaly a) -> Sev.valueOf(a.severity()).rank).reversed().thenComparing(Anomaly::at, Comparator.reverseOrder()))
                .limit(10).toList();
    }

    private enum Sev {
        LOW(1), MEDIUM(2), HIGH(3);
        final int rank;
        Sev(int rank) { this.rank = rank; }
    }

    private static void put(Map<UUID, Anomaly> map, Transaction t, String kind, String severity, String message) {
        Anomaly existing = map.get(t.getId());
        if (existing != null && Sev.valueOf(existing.severity()).rank >= Sev.valueOf(severity).rank) return;
        map.put(t.getId(), new Anomaly(t.getId(), t.getReference(), kind, severity, message, t.getAmount(), t.getCreatedAt()));
    }

    /** Recurring = at least 3 charges from one counterparty, amounts within 5% of the median, roughly weekly or monthly spacing. */
    List<Subscription> subscriptions(List<Transaction> debits90d) {
        Map<String, List<Transaction>> byWho = new LinkedHashMap<>();
        for (Transaction t : debits90d) {
            if (t.getCounterpartyName() == null) continue;
            byWho.computeIfAbsent(t.getCounterpartyName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(t);
        }
        List<Subscription> out = new ArrayList<>();
        byWho.forEach((who, list) -> {
            if (list.size() < 3) return;
            BigDecimal med = Stats.median(list.stream().map(Transaction::getAmount).toList());
            BigDecimal tol = med.multiply(new BigDecimal("0.05"));
            boolean steady = list.stream().allMatch(t -> t.getAmount().subtract(med).abs().compareTo(tol) <= 0);
            if (!steady) return;
            List<Long> gaps = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                gaps.add(ChronoUnit.DAYS.between(list.get(i - 1).getCreatedAt(), list.get(i).getCreatedAt()));
            }
            double gap = Stats.medianLong(gaps);
            String freq = gap >= 26 && gap <= 35 ? "MONTHLY" : (gap >= 6 && gap <= 8 ? "WEEKLY" : null);
            if (freq == null) return;
            Transaction last = list.get(list.size() - 1);
            LocalDate next = last.getCreatedAt().atZone(zone).toLocalDate().plusDays("MONTHLY".equals(freq) ? 30 : 7);
            out.add(new Subscription(last.getCounterpartyName(), med, freq, last.getCreatedAt(), next, list.size()));
        });
        out.sort(Comparator.comparing(Subscription::nextExpected));
        return out;
    }

    List<BudgetStatus> budgetStatuses(UUID userId, List<Transaction> debits, Instant now) {
        LocalDate today = now.atZone(zone).toLocalDate();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Map<String, BigDecimal> spentBy = new HashMap<>();
        debits.stream().filter(t -> !t.getCreatedAt().isBefore(monthStart))
                .forEach(t -> spentBy.merge(t.getCategory().name(), t.getAmount(), BigDecimal::add));
        List<BudgetStatus> out = new ArrayList<>();
        for (Budget b : budgets.findByUserIdOrderByCategoryAsc(userId)) {
            BigDecimal spent = spentBy.getOrDefault(b.getCategory(), Money.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = Money.percent(spent, b.getMonthlyLimit());
            String state = b.getMonthlyLimit().signum() == 0 || spent.compareTo(b.getMonthlyLimit()) > 0 ? "EXCEEDED"
                    : (pct.compareTo(new BigDecimal("80")) >= 0 ? "WARNING" : "OK");
            out.add(new BudgetStatus(b.getCategory(), b.getMonthlyLimit(), spent, b.getMonthlyLimit().subtract(spent), pct, state));
        }
        return out;
    }
}
