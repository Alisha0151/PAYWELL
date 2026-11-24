package com.paywell.wallet;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record WalletView(
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        String currency, String kind, boolean simulated,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal perPaymentLimit, String notice) {
    public static final String NOTICE = "Simulated demo-wallet funds. This is not a bank account and no real money moves.";
}
