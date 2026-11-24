package com.paywell.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywell.TestProps;
import com.paywell.audit.AuditService;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.IdempotencyService;
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
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import com.paywell.user.UserProfile;
import com.paywell.user.UserProfileRepository;
import com.paywell.user.UserRepository;
import com.paywell.wallet.WalletService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Orchestration-level tests with collaborators mocked. Locking, SQL and constraints are covered by the Testcontainers IT. */
class PaymentServiceTest {
    private IdempotencyService idempotency;
    private WalletService wallet;
    private TransactionRepository transactions;
    private RecipientRepository recipients;
    private UserProfileRepository profiles;
    private PaymentProvider provider;
    private RiskService risk;
    private PaymentService service;
    private final UUID me = UUID.randomUUID();
    private static final String KEY = "0b7d7a14-6f5e-4f43-bf53-3c1a2f9c1e11";

    @BeforeEach
    void setUp() {
        idempotency = mock(IdempotencyService.class);
        wallet = mock(WalletService.class);
        transactions = mock(TransactionRepository.class);
        recipients = mock(RecipientRepository.class);
        profiles = mock(UserProfileRepository.class);
        provider = mock(PaymentProvider.class);
        risk = mock(RiskService.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(idempotency.requireValid(KEY)).thenReturn(KEY);
        service = new PaymentService(idempotency, wallet, transactions, recipients, profiles, mock(UserRepository.class),
                mock(PaymentAttemptRepository.class), provider, risk, mock(RewardService.class), mock(NotificationService.class),
                mock(AuditService.class), mock(EventService.class), TestProps.create(), TestProps.CLOCK,
                new ReferenceGenerator(TestProps.CLOCK), new TransactionTemplate(tm));
    }

    private SendPaymentRequest req(String handle, String amount) {
        return new SendPaymentRequest(null, handle, null, new BigDecimal(amount), null, null, null);
    }

    @Test
    void retryWithSameKeyReplaysOriginalResultWithoutTouchingTheWalletOrProvider() {
        UUID txId = UUID.randomUUID();
        Transaction original = new Transaction(txId, "PW2611ABCDEFGH", me, UUID.randomUUID(), Direction.DEBIT, TxType.P2P_SEND,
                Category.TRANSFERS, new BigDecimal("25.00"), "INR", TxStatus.SUCCESS, "alice", "alice", null, null, Instant.now());
        when(recipients.findByUserIdAndHandle(me, "alice")).thenReturn(Optional.empty());
        when(profiles.findByHandle("alice")).thenReturn(Optional.empty());
        when(idempotency.findReplay(any(), any(), any())).thenReturn(Optional.of(txId));
        when(transactions.findByIdAndUserId(txId, me)).thenReturn(Optional.of(original));

        PaymentResponse r = service.send(me, KEY, req("Alice", "25.00"));

        assertThat(r.replayed()).isTrue();
        assertThat(r.transaction().id()).isEqualTo(txId);
        verify(wallet, never()).lock(any());
        verify(provider, never()).execute(any());
        verify(risk, never()).assess(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void missingIdempotencyKeyIsRejectedBeforeAnythingElse() {
        when(idempotency.requireValid(null)).thenThrow(new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
        assertThatThrownBy(() -> service.send(me, null, req("alice", "10.00")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
        verify(provider, never()).execute(any());
    }

    @Test
    void amountAboveLimitIsRejected() {
        assertThatThrownBy(() -> service.send(me, KEY, req("alice", "50000.01")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.AMOUNT_LIMIT_EXCEEDED));
    }

    @Test
    void payingYourOwnHandleIsRejected() {
        when(recipients.findByUserIdAndHandle(me, "me.self")).thenReturn(Optional.empty());
        when(profiles.findByHandle("me.self")).thenReturn(Optional.of(new UserProfile(me, "me.self", "INR")));
        assertThatThrownBy(() -> service.send(me, KEY, req("me.self", "10.00")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_PAY_SELF));
    }

    @Test
    void blockedByRiskNeverReachesTheProvider() {
        when(recipients.findByUserIdAndHandle(me, "bob")).thenReturn(Optional.empty());
        when(profiles.findByHandle("bob")).thenReturn(Optional.empty());
        when(idempotency.findReplay(any(), any(), any())).thenReturn(Optional.empty());
        when(risk.assess(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new RiskAssessment(90, "HIGH", "BLOCK", java.util.List.of(), "FRAUD_SERVICE", RiskAssessment.DISCLAIMER));
        when(wallet.lock(me)).thenReturn(new com.paywell.wallet.Account(UUID.randomUUID(), me, "INR", Instant.now()));

        assertThatThrownBy(() -> service.send(me, KEY, req("bob", "10.00")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYMENT_BLOCKED));
        verify(provider, never()).execute(any());
    }

    @Test
    void stepUpRequiredUnlessAcknowledged() {
        when(recipients.findByUserIdAndHandle(me, "bob")).thenReturn(Optional.empty());
        when(profiles.findByHandle("bob")).thenReturn(Optional.empty());
        when(idempotency.findReplay(any(), any(), any())).thenReturn(Optional.empty());
        when(risk.assess(any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new RiskAssessment(60, "MEDIUM", "CHALLENGE", java.util.List.of(), "LOCAL_FALLBACK", RiskAssessment.DISCLAIMER));
        assertThatThrownBy(() -> service.send(me, KEY, req("bob", "10.00")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.STEP_UP_REQUIRED));
        verify(provider, never()).execute(any());
    }
}
