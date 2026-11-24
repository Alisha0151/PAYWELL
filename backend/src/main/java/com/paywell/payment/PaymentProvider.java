package com.paywell.payment;

import java.math.BigDecimal;

/**
 * Port to whatever actually executes a payment. Only a SIMULATED implementation exists. A real provider would require a
 * signed agreement with a regulated partner, its own compliance review, and is deliberately not implemented here.
 */
public interface PaymentProvider {
    String name();

    ProviderResult execute(PaymentInstruction instruction);

    record PaymentInstruction(String reference, BigDecimal amount, String currency, String counterpartyHandle) {}

    record ProviderResult(boolean success, String providerReference, String failureCode) {
        public static ProviderResult ok(String ref) { return new ProviderResult(true, ref, null); }
        public static ProviderResult declined(String code) { return new ProviderResult(false, null, code); }
    }
}
