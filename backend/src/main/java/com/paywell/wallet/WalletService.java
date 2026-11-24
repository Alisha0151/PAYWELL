package com.paywell.wallet;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Money;
import com.paywell.config.PaywellProperties;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.ReferenceGenerator;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place balances change. Every change is: lock account row -> check -> mutate -> insert ledger row,
 * all inside the caller's transaction (MANDATORY), so a balance can never move without its ledger entry.
 */
@Service
public class WalletService {
    private final AccountRepository accounts;
    private final LedgerRepository ledger;
    private final TransactionRepository transactions;
    private final ReferenceGenerator references;
    private final PaywellProperties props;
    private final Clock clock;

    public WalletService(AccountRepository accounts, LedgerRepository ledger, TransactionRepository transactions,
                         ReferenceGenerator references, PaywellProperties props, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transactions = transactions;
        this.references = references;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public Account createAccount(UUID userId) {
        return accounts.save(new Account(UUID.randomUUID(), userId, props.payments().currency(), Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public Account get(UUID userId) {
        return accounts.findByUserId(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Wallet not found."));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Account lock(UUID userId) {
        return accounts.findByUserIdForUpdate(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Wallet not found."));
    }

    /** Applies a debit or credit to an already-locked account and records the ledger row. Amount must be scale-2 positive. */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry post(Account lockedAccount, Direction direction, BigDecimal amount, UUID transactionId, Instant at) {
        Money.requirePositive(amount);
        if (direction == Direction.DEBIT) {
            lockedAccount.debit(amount, at);
        } else {
            lockedAccount.credit(amount, at);
        }
        accounts.save(lockedAccount);
        return ledger.save(new LedgerEntry(lockedAccount.getId(), transactionId, direction, amount, lockedAccount.getBalance(), at));
    }

    /**
     * Records an already-successful wallet movement as a transaction + ledger row (top-ups, rewards, demo seeds, incoming).
     * The account must be locked by the caller.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Transaction postSimple(Account lockedAccount, Direction direction, TxType type, Category category, BigDecimal amount,
                                  String counterpartyName, String counterpartyHandle, String note, Instant at) {
        Transaction t = new Transaction(UUID.randomUUID(), references.next(), lockedAccount.getUserId(), lockedAccount.getId(),
                direction, type, category, amount, lockedAccount.getCurrency(), TxStatus.PENDING, counterpartyName,
                counterpartyHandle, null, note, at);
        transactions.save(t);
        post(lockedAccount, direction, amount, t.getId(), at);
        t.markSuccess(at);
        return transactions.save(t);
    }

    /** SIMULATED top-up: creates demo funds. No payment instrument is charged. */
    @Transactional
    public Transaction simulatedTopUp(UUID userId, BigDecimal amount) {
        if (!props.demo().enabled()) {
            throw new ApiException(ErrorCode.FEATURE_DISABLED, "Simulated top-ups are disabled in this environment.");
        }
        BigDecimal a = Money.requireWithinLimit(amount, props.payments().maxAmount());
        Account acc = lock(userId);
        return postSimple(acc, Direction.CREDIT, TxType.TOPUP, Category.TOPUP, a, "PAYWELL demo funds", null,
                "SIMULATED top-up (no real money)", Instant.now(clock));
    }

    /** SIMULATED incoming transfer from a fictional sender (demo mode only), to exercise the RECEIVE flow. */
    @Transactional
    public Transaction simulatedIncoming(UUID userId, BigDecimal amount, String fromName) {
        if (!props.demo().enabled()) {
            throw new ApiException(ErrorCode.FEATURE_DISABLED, "Simulated incoming payments are disabled in this environment.");
        }
        BigDecimal a = Money.requireWithinLimit(amount, props.payments().maxAmount());
        Account acc = lock(userId);
        String name = fromName == null || fromName.isBlank() ? "Demo Friend" : fromName.trim();
        return postSimple(acc, Direction.CREDIT, TxType.P2P_RECEIVE, Category.TRANSFERS, a, name, null,
                "SIMULATED incoming payment (no real money)", Instant.now(clock));
    }
}
