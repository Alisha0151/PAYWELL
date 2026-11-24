package com.paywell.wallet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paywell.common.IdempotencyService;
import com.paywell.common.PageResponse;
import com.paywell.config.PaywellProperties;
import com.paywell.security.CurrentUser;
import com.paywell.transaction.Direction;
import com.paywell.transaction.TransactionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private final WalletService wallet;
    private final WalletTopUpService topUps;
    private final LedgerRepository ledger;
    private final PaywellProperties props;

    public WalletController(WalletService wallet, WalletTopUpService topUps, LedgerRepository ledger, PaywellProperties props) {
        this.wallet = wallet;
        this.topUps = topUps;
        this.ledger = ledger;
        this.props = props;
    }

    public record TopUpRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount) {}

    public record LedgerView(Long id, Direction direction,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balanceAfter, Instant createdAt) {}

    @GetMapping
    public WalletView get() {
        Account a = wallet.get(CurrentUser.id());
        return new WalletView(a.getBalance(), a.getCurrency(), a.getKind(), true, props.payments().maxAmount(), WalletView.NOTICE);
    }

    /** SIMULATED funds only. Requires Idempotency-Key so a retried request cannot add funds twice. */
    @PostMapping("/top-up")
    public TransactionView topUp(@RequestHeader(value = IdempotencyService.HEADER, required = false) String key,
                                 @Valid @RequestBody TopUpRequest req) {
        return topUps.topUp(CurrentUser.id(), key, req.amount());
    }

    @GetMapping("/ledger")
    public PageResponse<LedgerView> ledger(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Account a = wallet.get(CurrentUser.id());
        var p = ledger.findByAccountIdOrderByIdDesc(a.getId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(p, e -> new LedgerView(e.getId(), e.getDirection(), e.getAmount(), e.getBalanceAfter(), e.getCreatedAt()));
    }
}
