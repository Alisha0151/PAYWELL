package com.paywell.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywell.TestProps;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.transaction.Direction;
import com.paywell.transaction.ReferenceGenerator;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WalletServiceTest {
    private AccountRepository accounts;
    private LedgerRepository ledger;
    private TransactionRepository transactions;
    private WalletService service;
    private final UUID userId = UUID.randomUUID();
    private Account account;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        ledger = mock(LedgerRepository.class);
        transactions = mock(TransactionRepository.class);
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactions.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        service = new WalletService(accounts, ledger, transactions, new ReferenceGenerator(TestProps.CLOCK), TestProps.create(true, true), TestProps.CLOCK);
        account = new Account(UUID.randomUUID(), userId, "INR", Instant.now());
    }

    @Test
    void creditIncreasesBalanceAndWritesLedgerRowWithBalanceAfter() {
        service.post(account, Direction.CREDIT, new BigDecimal("100.00"), UUID.randomUUID(), Instant.now());
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        ArgumentCaptor<LedgerEntry> c = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledger).save(c.capture());
        assertThat(c.getValue().getDirection()).isEqualTo(Direction.CREDIT);
        assertThat(c.getValue().getAmount()).isEqualByComparingTo("100.00");
        assertThat(c.getValue().getBalanceAfter()).isEqualByComparingTo("100.00");
    }

    @Test
    void debitReducesBalanceExactly() {
        service.post(account, Direction.CREDIT, new BigDecimal("100.10"), UUID.randomUUID(), Instant.now());
        service.post(account, Direction.DEBIT, new BigDecimal("0.20"), UUID.randomUUID(), Instant.now());
        assertThat(account.getBalance()).isEqualByComparingTo("99.90");
    }

    @Test
    void debitBeyondBalanceFailsAndWritesNothing() {
        service.post(account, Direction.CREDIT, new BigDecimal("50.00"), UUID.randomUUID(), Instant.now());
        org.mockito.Mockito.clearInvocations(ledger);
        assertThatThrownBy(() -> service.post(account, Direction.DEBIT, new BigDecimal("50.01"), UUID.randomUUID(), Instant.now()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
        verify(ledger, never()).save(any());
    }

    @Test
    void rejectsNonPositiveAndSubPaisaAmounts() {
        for (String bad : new String[]{"0", "-5.00", "1.005"}) {
            assertThatThrownBy(() -> service.post(account, Direction.CREDIT, new BigDecimal(bad), UUID.randomUUID(), Instant.now()))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_AMOUNT));
        }
    }

    @Test
    void simulatedTopUpCreatesSuccessfulSimulatedTransactionAndLedgerRow() {
        when(accounts.findByUserIdForUpdate(userId)).thenReturn(java.util.Optional.of(account));
        Transaction t = service.simulatedTopUp(userId, new BigDecimal("250"));
        assertThat(t.getStatus()).isEqualTo(TxStatus.SUCCESS);
        assertThat(t.isSimulated()).isTrue();
        assertThat(t.getNote()).contains("SIMULATED");
        assertThat(account.getBalance()).isEqualByComparingTo("250.00");
        verify(ledger).save(any(LedgerEntry.class));
    }

    @Test
    void topUpIsRefusedWhenDemoModeIsOff() {
        WalletService off = new WalletService(accounts, ledger, transactions, new ReferenceGenerator(TestProps.CLOCK),
                TestProps.create(false, true), TestProps.CLOCK);
        assertThatThrownBy(() -> off.simulatedTopUp(userId, new BigDecimal("10")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.FEATURE_DISABLED));
        verify(accounts, never()).findByUserIdForUpdate(any());
    }

    @Test
    void topUpAboveDemoLimitIsRefused() {
        assertThatThrownBy(() -> service.simulatedTopUp(userId, new BigDecimal("50000.01")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.AMOUNT_LIMIT_EXCEEDED));
    }
}
