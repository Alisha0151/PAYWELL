package com.paywell.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SimulatedPaymentProviderTest {
    private final SimulatedPaymentProvider provider = new SimulatedPaymentProvider();

    private PaymentProvider.PaymentInstruction to(String handle) {
        return new PaymentProvider.PaymentInstruction("PW2611ABCDEFGH", new BigDecimal("10.00"), "INR", handle);
    }

    @Test
    void succeedsWithSimulatedReference() {
        var r = provider.execute(to("alice"));
        assertThat(r.success()).isTrue();
        assertThat(r.providerReference()).startsWith("SIM-");
        assertThat(provider.name()).isEqualTo("SIMULATED");
    }

    @Test
    void declinesHandlesThatAskForIt() {
        var r = provider.execute(to("Please.Decline"));
        assertThat(r.success()).isFalse();
        assertThat(r.failureCode()).isEqualTo("SIMULATED_DECLINE");
    }
}
