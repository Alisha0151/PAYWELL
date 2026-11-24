package com.paywell.meta;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paywell.auth.OtpService;
import com.paywell.config.PaywellProperties;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public capability/honesty endpoint so clients can show accurate banners (demo mode, simulated provider, OTP delivery). */
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {
    public record Meta(String name, String version, boolean demoMode, String paymentProvider, boolean otpDeliveryConfigured,
                       String currency, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal perPaymentLimit, List<String> notices) {}

    private final PaywellProperties props;
    private final OtpService otp;

    public MetaController(PaywellProperties props, OtpService otp) {
        this.props = props;
        this.otp = otp;
    }

    @GetMapping
    public Meta meta() {
        return new Meta("PAYWELL", "0.1.0", props.demo().enabled(), "SIMULATED", otp.deliveryConfigured(), props.payments().currency(),
                props.payments().maxAmount(), List.of(
                        "Payments are simulated. No real money moves and balances are demo-wallet funds.",
                        "PAYWELL is a demonstration product and is not a bank or a licensed payment service.",
                        "One-time-code delivery is only available when a delivery provider is configured."));
    }
}
