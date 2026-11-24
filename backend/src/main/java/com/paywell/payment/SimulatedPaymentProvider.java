package com.paywell.payment;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic fake provider: succeeds unless the counterparty handle contains "decline" (lets testers exercise the
 * failure path). It performs no network calls and moves no real money.
 */
@Component
@ConditionalOnProperty(name = "paywell.payments.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedPaymentProvider implements PaymentProvider {
    public static final String NAME = "SIMULATED";

    @Override
    public String name() { return NAME; }

    @Override
    public ProviderResult execute(PaymentInstruction i) {
        String h = i.counterpartyHandle() == null ? "" : i.counterpartyHandle().toLowerCase(Locale.ROOT);
        if (h.contains("decline")) {
            return ProviderResult.declined("SIMULATED_DECLINE");
        }
        return ProviderResult.ok("SIM-" + i.reference());
    }
}
