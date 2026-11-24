package com.paywell.demo;

import com.paywell.common.IdempotencyService;
import com.paywell.security.CurrentUser;
import com.paywell.transaction.TransactionView;
import com.paywell.wallet.WalletTopUpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demo-only helpers (return 403 FEATURE_DISABLED when paywell.demo.enabled=false). */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {
    public record SimulateIncomingRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                                          @Size(max = 120) String fromName) {}

    private final WalletTopUpService service;

    public DemoController(WalletTopUpService service) {
        this.service = service;
    }

    @PostMapping("/simulate-incoming")
    public TransactionView simulateIncoming(@RequestHeader(value = IdempotencyService.HEADER, required = false) String key,
                                            @Valid @RequestBody SimulateIncomingRequest r) {
        return service.simulateIncoming(CurrentUser.id(), key, r.amount(), r.fromName());
    }
}
