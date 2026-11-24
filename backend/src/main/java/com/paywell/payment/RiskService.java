package com.paywell.payment;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.config.PaywellProperties;
import com.paywell.transaction.Direction;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the user's behavioural context, asks the fraud service, and falls back to simple local rules if it is down. */
@Service
public class RiskService {
    static final BigDecimal LARGE = new BigDecimal("25000");
    static final BigDecimal HIGH_VALUE_FAIL_CLOSED = new BigDecimal("10000");

    private final FraudClient fraud;
    private final TransactionRepository transactions;
    private final PaywellProperties props;
    private final Clock clock;
    private final ZoneId zone;

    public RiskService(FraudClient fraud, TransactionRepository transactions, PaywellProperties props, Clock clock, ZoneId userZone) {
        this.fraud = fraud;
        this.transactions = transactions;
        this.props = props;
        this.clock = clock;
        this.zone = userZone;
    }

    @Transactional(readOnly = true)
    public RiskAssessment assess(UUID userId, BigDecimal amount, boolean newRecipient) {
        Instant now = Instant.now(clock);
        List<Transaction> recent = transactions.findByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                userId, TxStatus.SUCCESS, now.minus(Duration.ofDays(30)));
        List<BigDecimal> debits = recent.stream().filter(t -> t.getDirection() == Direction.DEBIT).map(Transaction::getAmount).toList();
        BigDecimal avg = debits.isEmpty() ? BigDecimal.ZERO
                : debits.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(debits.size()), 2, RoundingMode.HALF_UP);
        int lastHour = (int) recent.stream().filter(t -> t.getDirection() == Direction.DEBIT && t.getCreatedAt().isAfter(now.minus(Duration.ofHours(1)))).count();
        BigDecimal last24 = recent.stream().filter(t -> t.getDirection() == Direction.DEBIT && t.getCreatedAt().isAfter(now.minus(Duration.ofHours(24))))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int hour = now.atZone(zone).getHour();
        var req = new FraudClient.RiskRequest(amount, props.payments().currency(), avg, lastHour, last24, newRecipient, true, hour);
        return fraud.assess(req).orElseGet(() -> fallback(amount, avg));
    }

    RiskAssessment fallback(BigDecimal amount, BigDecimal avg30d) {
        if (!props.fraud().failOpen() && amount.compareTo(HIGH_VALUE_FAIL_CLOSED) >= 0) {
            throw new ApiException(ErrorCode.RISK_UNAVAILABLE, "Risk check is unavailable; high-value payments are paused.");
        }
        List<RiskAssessment.Factor> f = new ArrayList<>();
        int score = 5;
        if (amount.compareTo(LARGE) >= 0) {
            score += 55;
            f.add(new RiskAssessment.Factor("LARGE_AMOUNT", 55, "Very large single payment."));
        }
        if (avg30d.signum() > 0 && amount.compareTo(avg30d.multiply(BigDecimal.TEN)) >= 0) {
            score += 30;
            f.add(new RiskAssessment.Factor("AMOUNT_VS_HISTORY", 30, "Amount is at least 10x your 30-day average."));
        }
        String category = score >= 60 ? "MEDIUM" : "LOW";
        String action = score >= 60 ? "CHALLENGE" : "ALLOW";
        return new RiskAssessment(Math.min(score, 100), category, action, f, "LOCAL_FALLBACK", RiskAssessment.DISCLAIMER);
    }
}
