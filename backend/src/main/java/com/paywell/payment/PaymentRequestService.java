package com.paywell.payment;

import com.paywell.audit.AuditService;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Money;
import com.paywell.config.PaywellProperties;
import com.paywell.notification.NotificationService;
import com.paywell.payment.PaymentDtos.PaymentRequestView;
import com.paywell.payment.PaymentDtos.PaymentResponse;
import com.paywell.payment.PaymentDtos.RequestMoneyRequest;
import com.paywell.payment.PaymentDtos.SendPaymentRequest;
import com.paywell.transaction.TxStatus;
import com.paywell.user.UserProfile;
import com.paywell.user.UserProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** "Request money": the RECEIVE side. Settling a request goes through the normal (simulated, idempotent) payment path. */
@Service
public class PaymentRequestService {
    private final PaymentRequestRepository repository;
    private final UserProfileRepository profiles;
    private final PaymentService payments;
    private final NotificationService notifications;
    private final AuditService audit;
    private final PaywellProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public PaymentRequestService(PaymentRequestRepository repository, UserProfileRepository profiles, PaymentService payments,
                                 NotificationService notifications, AuditService audit, PaywellProperties props, Clock clock,
                                 TransactionTemplate tx) {
        this.repository = repository;
        this.profiles = profiles;
        this.payments = payments;
        this.notifications = notifications;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    @Transactional
    public PaymentRequestView create(UUID userId, RequestMoneyRequest req) {
        BigDecimal amount = Money.requireWithinLimit(req.amount(), props.payments().maxAmount());
        String handle = req.payerHandle().trim().toLowerCase(Locale.ROOT);
        UUID payerId = profiles.findByHandle(handle).map(UserProfile::getUserId).orElse(null);
        if (userId.equals(payerId)) {
            throw new ApiException(ErrorCode.CANNOT_PAY_SELF, "You cannot request money from yourself.");
        }
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        PaymentRequestEntity e = repository.save(new PaymentRequestEntity(userId, handle, payerId, amount,
                props.payments().currency(), note, Instant.now(clock)));
        if (payerId != null) {
            String from = profiles.findById(userId).map(UserProfile::getHandle).orElse("someone");
            notifications.notify(payerId, "PAYMENT", "Payment request", from + " requested " + amount.toPlainString() + ".");
        }
        audit.record(userId, "PAYMENT_REQUEST_CREATED", "PaymentRequest", e.getId().toString(), "amount=" + amount.toPlainString());
        return toView(e, "OUTGOING", userId);
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestView> list(UUID userId, String direction) {
        boolean incoming = "incoming".equalsIgnoreCase(direction);
        List<PaymentRequestEntity> rows = incoming ? repository.findByPayerUserIdOrderByCreatedAtDesc(userId)
                : repository.findByRequesterUserIdOrderByCreatedAtDesc(userId);
        return rows.stream().map(r -> toView(r, incoming ? "INCOMING" : "OUTGOING", userId)).toList();
    }

    /** Pays an incoming request: same validation, risk, idempotency and ledger rules as any other payment. */
    public PaymentResponse pay(UUID userId, UUID requestId, String idemKey) {
        PaymentRequestEntity r = tx.execute(s -> repository.findByIdAndPayerUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Payment request not found.")));
        if (!r.isPending() && !"PAID".equals(r.getStatus())) {
            throw new ApiException(ErrorCode.REQUEST_NOT_PENDING);
        }
        String requesterHandle = profiles.findById(r.getRequesterUserId()).map(UserProfile::getHandle)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Requester not found."));
        PaymentResponse res = payments.send(userId, idemKey, new SendPaymentRequest(null, requesterHandle, null, r.getAmount(),
                r.getNote() == null ? "Payment request" : r.getNote(), null, Boolean.FALSE));
        if (res.transaction().status() == TxStatus.SUCCESS) {
            tx.executeWithoutResult(s -> repository.findById(requestId).ifPresent(x -> {
                if (x.isPending()) x.markPaid(res.transaction().id(), Instant.now(clock));
            }));
        }
        return res;
    }

    @Transactional
    public PaymentRequestView decline(UUID userId, UUID requestId) {
        PaymentRequestEntity r = repository.findByIdAndPayerUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Payment request not found."));
        if (!r.isPending()) throw new ApiException(ErrorCode.REQUEST_NOT_PENDING);
        r.markDeclined(Instant.now(clock));
        return toView(r, "INCOMING", userId);
    }

    @Transactional
    public PaymentRequestView cancel(UUID userId, UUID requestId) {
        PaymentRequestEntity r = repository.findByIdAndRequesterUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Payment request not found."));
        if (!r.isPending()) throw new ApiException(ErrorCode.REQUEST_NOT_PENDING);
        r.markCancelled(Instant.now(clock));
        return toView(r, "OUTGOING", userId);
    }

    private PaymentRequestView toView(PaymentRequestEntity r, String direction, UUID me) {
        String counterparty = "INCOMING".equals(direction)
                ? profiles.findById(r.getRequesterUserId()).map(UserProfile::getHandle).orElse("unknown")
                : r.getPayerHandle();
        return new PaymentRequestView(r.getId(), direction, counterparty, r.getAmount(), r.getCurrency(), r.getNote(), r.getStatus(),
                r.getCreatedAt(), r.getResolvedAt(), r.getPaidTransactionId());
    }
}
