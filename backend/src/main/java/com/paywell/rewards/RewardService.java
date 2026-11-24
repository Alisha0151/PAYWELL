package com.paywell.rewards;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paywell.audit.AuditService;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Money;
import com.paywell.notification.NotificationService;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionView;
import com.paywell.transaction.TxType;
import com.paywell.wallet.Account;
import com.paywell.wallet.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Demo rewards. Cashback is credited to the SIMULATED wallet only; it has no cash value. */
@Service
public class RewardService {
    static final BigDecimal CASHBACK_RATE = new BigDecimal("0.005");
    static final BigDecimal CASHBACK_MIN_PAYMENT = new BigDecimal("100.00");
    static final BigDecimal CASHBACK_CAP = new BigDecimal("20.00");
    public static final BigDecimal WELCOME_AMOUNT = new BigDecimal("50.00");

    private final RewardRepository repository;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public RewardService(RewardRepository repository, WalletService wallet, NotificationService notifications,
                         AuditService audit, Clock clock) {
        this.repository = repository;
        this.wallet = wallet;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    public record RewardView(UUID id, String kind, String title, String description,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String status,
                             Instant createdAt, Instant expiresAt, Instant redeemedAt) {
        static RewardView of(Reward r) {
            return new RewardView(r.getId(), r.getKind(), r.getTitle(), r.getDescription(), r.getAmount(), r.getStatus(),
                    r.getCreatedAt(), r.getExpiresAt(), r.getRedeemedAt());
        }
    }

    public record RewardsSummary(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal availableTotal,
                                 @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal redeemedTotal,
                                 List<RewardView> rewards, String notice) {}

    /** Pure calculation, unit-tested: 0.5% of payments >= 100.00, capped at 20.00, rounded down to the paisa. */
    public static BigDecimal cashbackFor(BigDecimal paymentAmount) {
        if (paymentAmount.compareTo(CASHBACK_MIN_PAYMENT) < 0) return Money.ZERO;
        BigDecimal c = paymentAmount.multiply(CASHBACK_RATE).setScale(Money.SCALE, RoundingMode.DOWN);
        return c.min(CASHBACK_CAP);
    }

    @Transactional
    public void grantCashback(Transaction payment) {
        BigDecimal c = cashbackFor(payment.getAmount());
        if (c.signum() <= 0) return;
        Instant now = Instant.now(clock);
        repository.save(new Reward(payment.getUserId(), "CASHBACK", "Cashback on your payment",
                "0.5% demo cashback on " + payment.getReference(), c, payment.getId(), now, now.plus(Duration.ofDays(60))));
        notifications.notify(payment.getUserId(), "REWARD", "Cashback earned", "You earned demo cashback of " + c.toPlainString() + ".");
    }

    @Transactional
    public void grantWelcome(UUID userId) {
        Instant now = Instant.now(clock);
        repository.save(new Reward(userId, "WELCOME", "Welcome reward", "Demo welcome reward for joining PAYWELL.",
                WELCOME_AMOUNT, null, now, now.plus(Duration.ofDays(30))));
    }

    @Transactional(readOnly = true)
    public RewardsSummary summary(UUID userId) {
        Instant now = Instant.now(clock);
        List<Reward> all = repository.findByUserIdOrderByCreatedAtDesc(userId);
        BigDecimal available = Money.sum(all.stream().filter(r -> r.isRedeemable(now)).map(Reward::getAmount).toList());
        BigDecimal redeemed = Money.sum(all.stream().filter(r -> "REDEEMED".equals(r.getStatus())).map(Reward::getAmount).toList());
        return new RewardsSummary(available, redeemed, all.stream().map(RewardView::of).toList(),
                "Demo rewards are credited to the simulated wallet and have no cash value.");
    }

    /** Redeems into the demo wallet. The reward row is locked, so a double tap cannot credit twice. */
    @Transactional
    public TransactionView redeem(UUID userId, UUID rewardId) {
        Account acc = wallet.lock(userId);
        Reward r = repository.findForUpdate(rewardId, userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Reward not found."));
        Instant now = Instant.now(clock);
        if (!r.isRedeemable(now)) {
            throw new ApiException(ErrorCode.REWARD_NOT_AVAILABLE);
        }
        Transaction t = wallet.postSimple(acc, Direction.CREDIT, TxType.REWARD, Category.REWARDS, r.getAmount(),
                "PAYWELL Rewards", null, "Reward redeemed (simulated wallet credit)", now);
        r.redeem(now);
        audit.record(userId, "REWARD_REDEEMED", "Reward", r.getId().toString(), "amount=" + r.getAmount().toPlainString());
        return TransactionView.of(t);
    }
}
