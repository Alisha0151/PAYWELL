package com.paywell.wallet;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.IdempotencyService;
import com.paywell.common.Money;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TransactionView;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WalletTopUpService {
    private final WalletService wallet;
    private final IdempotencyService idempotency;
    private final TransactionRepository transactions;
    private final TransactionTemplate tx;

    public WalletTopUpService(WalletService wallet, IdempotencyService idempotency, TransactionRepository transactions,
                              TransactionTemplate tx) {
        this.wallet = wallet;
        this.idempotency = idempotency;
        this.transactions = transactions;
        this.tx = tx;
    }

    public TransactionView topUp(UUID userId, String rawKey, BigDecimal rawAmount) {
        return run(userId, rawKey, rawAmount, null);
    }

    /** Demo-only: credit the wallet as if a fictional person had paid you. */
    public TransactionView simulateIncoming(UUID userId, String rawKey, BigDecimal rawAmount, String fromName) {
        return run(userId, rawKey, rawAmount, fromName == null ? "Demo Friend" : fromName);
    }

    private TransactionView run(UUID userId, String rawKey, BigDecimal rawAmount, String incomingFrom) {
        String key = idempotency.requireValid(rawKey);
        BigDecimal amount = Money.requirePositive(rawAmount);
        String hash = IdempotencyService.hash(incomingFrom == null ? "TOPUP" : "INCOMING", amount.toPlainString(), incomingFrom);
        try {
            return tx.execute(status -> {
                wallet.lock(userId); // serialises concurrent requests of the same user
                Optional<UUID> replay = idempotency.findReplay(userId, key, hash);
                if (replay.isPresent()) {
                    return view(userId, replay.get());
                }
                Transaction t = incomingFrom == null ? wallet.simulatedTopUp(userId, amount) : wallet.simulatedIncoming(userId, amount, incomingFrom);
                idempotency.record(userId, key, hash, t.getId());
                return TransactionView.of(t);
            });
        } catch (DataIntegrityViolationException race) {
            return tx.execute(status -> idempotency.findReplay(userId, key, hash).map(id -> view(userId, id))
                    .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT)));
        }
    }

    private TransactionView view(UUID userId, UUID id) {
        return transactions.findByIdAndUserId(id, userId).map(TransactionView::of)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }
}
