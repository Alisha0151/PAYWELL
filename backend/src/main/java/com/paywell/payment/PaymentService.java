package com.paywell.payment;

import com.paywell.audit.AuditService;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.IdempotencyService;
import com.paywell.common.Money;
import com.paywell.config.PaywellProperties;
import com.paywell.events.EventService;
import com.paywell.notification.NotificationService;
import com.paywell.payment.PaymentDtos.PaymentResponse;
import com.paywell.payment.PaymentDtos.SendPaymentRequest;
import com.paywell.rewards.RewardService;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.ReferenceGenerator;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TransactionView;
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import com.paywell.user.User;
import com.paywell.user.UserProfile;
import com.paywell.user.UserProfileRepository;
import com.paywell.user.UserRepository;
import com.paywell.wallet.Account;
import com.paywell.wallet.WalletService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Send-money orchestration. Order of operations: validate -> idempotency fast-path -> risk check -> (one DB transaction:
 * lock wallets in a fixed order, re-check idempotency, balance check, provider call, ledger postings, idempotency record).
 * The provider is SIMULATED; no real money moves.
 */
@Service
public class PaymentService {
    private final IdempotencyService idempotency;
    private final WalletService wallet;
    private final TransactionRepository transactions;
    private final RecipientRepository recipients;
    private final UserProfileRepository profiles;
    private final UserRepository users;
    private final PaymentAttemptRepository attempts;
    private final PaymentProvider provider;
    private final RiskService risk;
    private final RewardService rewards;
    private final NotificationService notifications;
    private final AuditService audit;
    private final EventService events;
    private final PaywellProperties props;
    private final Clock clock;
    private final ReferenceGenerator references;
    private final TransactionTemplate tx;

    public PaymentService(IdempotencyService idempotency, WalletService wallet, TransactionRepository transactions,
                          RecipientRepository recipients, UserProfileRepository profiles, UserRepository users,
                          PaymentAttemptRepository attempts, PaymentProvider provider, RiskService risk, RewardService rewards,
                          NotificationService notifications, AuditService audit, EventService events, PaywellProperties props,
                          Clock clock, ReferenceGenerator references, TransactionTemplate tx) {
        this.idempotency = idempotency;
        this.wallet = wallet;
        this.transactions = transactions;
        this.recipients = recipients;
        this.profiles = profiles;
        this.users = users;
        this.attempts = attempts;
        this.provider = provider;
        this.risk = risk;
        this.rewards = rewards;
        this.notifications = notifications;
        this.audit = audit;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.references = references;
        this.tx = tx;
    }

    /** Resolved counterparty for one request (not an entity; may refer to a recipient that does not exist yet). */
    private record Target(Recipient existing, String handle, String name, UUID internalUserId) {}

    public PaymentResponse send(UUID userId, String rawKey, SendPaymentRequest req) {
        String key = idempotency.requireValid(rawKey);
        BigDecimal amount = Money.requireWithinLimit(req.amount(), props.payments().maxAmount());
        Target target = resolve(userId, req);
        String hash = IdempotencyService.hash("SEND", target.handle(), amount.toPlainString(), req.note(), req.category());

        Optional<TransactionView> early = tx.execute(s -> idempotency.findReplay(userId, key, hash).map(id -> view(userId, id)));
        if (early != null && early.isPresent()) {
            return new PaymentResponse(early.get(), true, null, PaymentResponse.NOTICE);
        }

        RiskAssessment assessment = risk.assess(userId, amount, target.existing() == null);
        if (assessment.blocks()) {
            recordBlocked(userId, key, hash, target, amount, req, assessment);
            throw new ApiException(ErrorCode.PAYMENT_BLOCKED, "This payment was stopped by risk checks.").with("risk", assessment);
        }
        if (assessment.needsStepUp() && !Boolean.TRUE.equals(req.acknowledgeRisk())) {
            throw new ApiException(ErrorCode.STEP_UP_REQUIRED,
                    "Please review and confirm this payment. Resend with acknowledgeRisk=true to proceed.").with("risk", assessment);
        }

        try {
            return tx.execute(s -> executeSend(userId, key, hash, target, amount, req, assessment));
        } catch (DataIntegrityViolationException race) {
            PaymentResponse replay = tx.execute(s -> idempotency.findReplay(userId, key, hash)
                    .map(id -> new PaymentResponse(view(userId, id), true, null, PaymentResponse.NOTICE))
                    .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT)));
            return replay;
        }
    }

    private PaymentResponse executeSend(UUID userId, String key, String hash, Target target, BigDecimal amount,
                                        SendPaymentRequest req, RiskAssessment assessment) {
        Instant now = Instant.now(clock);
        Account sender;
        Account receiver = null;
        UUID other = target.internalUserId();
        if (other != null) {   // lock both wallets in a deterministic order so A->B and B->A cannot deadlock
            if (userId.compareTo(other) < 0) {
                sender = wallet.lock(userId);
                receiver = wallet.lock(other);
            } else {
                receiver = wallet.lock(other);
                sender = wallet.lock(userId);
            }
        } else {
            sender = wallet.lock(userId);
        }

        Optional<UUID> replay = idempotency.findReplay(userId, key, hash);
        if (replay.isPresent()) {
            return new PaymentResponse(view(userId, replay.get()), true, null, PaymentResponse.NOTICE);
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        Recipient recipient = target.existing() != null ? target.existing()
                : recipients.save(new Recipient(userId, target.name(), target.handle(), now));
        Category category = Category.parseOrDefault(req.category(), Category.TRANSFERS);
        Transaction t = transactions.save(new Transaction(UUID.randomUUID(), references.next(), userId, sender.getId(), Direction.DEBIT,
                TxType.P2P_SEND, category, amount, sender.getCurrency(), TxStatus.PENDING, recipient.getName(), recipient.getHandle(),
                recipient.getId(), trimToNull(req.note()), now));

        PaymentProvider.ProviderResult pr = provider.execute(
                new PaymentProvider.PaymentInstruction(t.getReference(), amount, t.getCurrency(), t.getCounterpartyHandle()));
        attempts.save(new PaymentAttempt(t.getId(), provider.name(), pr.providerReference(), pr.success() ? "SUCCESS" : "FAILED",
                pr.failureCode(), assessment.score(), assessment.category(), assessment.action(), now));

        if (!pr.success()) {
            t.markFailed(pr.failureCode(), now);
            transactions.save(t);
            audit.record(userId, "PAYMENT_FAILED", "Transaction", t.getId().toString(), "ref=" + t.getReference() + " code=" + pr.failureCode());
            idempotency.record(userId, key, hash, t.getId());
            return new PaymentResponse(TransactionView.of(t), false, assessment, PaymentResponse.NOTICE);
        }

        wallet.post(sender, Direction.DEBIT, amount, t.getId(), now);
        t.markSuccess(now);
        transactions.save(t);

        if (receiver != null) {
            String senderHandle = profiles.findById(userId).map(UserProfile::getHandle).orElse(null);
            String senderName = users.findById(userId).map(User::getFullName).orElse("PAYWELL user");
            wallet.postSimple(receiver, Direction.CREDIT, TxType.P2P_RECEIVE, Category.TRANSFERS, amount, senderName, senderHandle,
                    trimToNull(req.note()), now);
            notifications.notify(other, "PAYMENT", "Money received (simulated)", senderName + " sent you " + amount.toPlainString() + ".");
        }
        rewards.grantCashback(t);
        notifications.notify(userId, "PAYMENT", "Payment sent (simulated)", amount.toPlainString() + " to " + t.getCounterpartyName() + ".");
        audit.record(userId, "PAYMENT_SENT", "Transaction", t.getId().toString(), "ref=" + t.getReference() + " amount=" + amount.toPlainString());
        idempotency.record(userId, key, hash, t.getId());
        events.publish("payment.completed", userId, t.getId().toString(),
                Map.of("reference", t.getReference(), "amount", amount.toPlainString(), "currency", t.getCurrency(), "simulated", true));
        return new PaymentResponse(TransactionView.of(t), false, assessment, PaymentResponse.NOTICE);
    }

    /** Persists the blocked attempt (history + idempotency) in its own transaction before the 422 is returned. */
    private void recordBlocked(UUID userId, String key, String hash, Target target, BigDecimal amount, SendPaymentRequest req,
                               RiskAssessment assessment) {
        tx.executeWithoutResult(s -> {
            Account sender = wallet.lock(userId);
            if (idempotency.findReplay(userId, key, hash).isPresent()) return;
            Instant now = Instant.now(clock);
            Transaction t = new Transaction(UUID.randomUUID(), references.next(), userId, sender.getId(), Direction.DEBIT, TxType.P2P_SEND,
                    Category.parseOrDefault(req.category(), Category.TRANSFERS), amount, sender.getCurrency(), TxStatus.PENDING,
                    target.name(), target.handle(), target.existing() == null ? null : target.existing().getId(), trimToNull(req.note()), now);
            t.markFailed("RISK_BLOCKED", now);
            transactions.save(t);
            attempts.save(new PaymentAttempt(t.getId(), provider.name(), null, "SKIPPED", "RISK_BLOCKED", assessment.score(),
                    assessment.category(), assessment.action(), now));
            audit.record(userId, "PAYMENT_BLOCKED", "Transaction", t.getId().toString(), "ref=" + t.getReference() + " score=" + assessment.score());
            idempotency.record(userId, key, hash, t.getId());
        });
    }

    private Target resolve(UUID userId, SendPaymentRequest req) {
        Recipient existing = null;
        String handle;
        String name;
        if (req.recipientId() != null) {
            existing = recipients.findByIdAndUserId(req.recipientId(), userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Recipient not found."));
            handle = existing.getHandle();
            name = existing.getName();
        } else if (req.handle() != null && !req.handle().isBlank()) {
            handle = req.handle().trim().toLowerCase(Locale.ROOT);
            existing = recipients.findByUserIdAndHandle(userId, handle).orElse(null);
            name = existing != null ? existing.getName() : (req.name() == null || req.name().isBlank() ? handle : req.name().trim());
        } else {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Provide recipientId or handle.");
        }
        UUID internal = profiles.findByHandle(handle).map(UserProfile::getUserId).orElse(null);
        if (userId.equals(internal)) {
            throw new ApiException(ErrorCode.CANNOT_PAY_SELF);
        }
        return new Target(existing, handle, name, internal);
    }

    private TransactionView view(UUID userId, UUID id) {
        return transactions.findByIdAndUserId(id, userId).map(TransactionView::of)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
