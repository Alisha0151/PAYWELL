package com.paywell.radar;

import com.paywell.common.Money;
import com.paywell.config.PaywellProperties;
import com.paywell.payment.PaymentDtos;
import com.paywell.payment.RecipientRepository;
import com.paywell.payment.RiskAssessment;
import com.paywell.payment.RiskService;
import com.paywell.radar.RadarDtos.RadarResponse;
import com.paywell.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/radar")
public class RadarController {
    public record CheckRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                               @Pattern(regexp = PaymentDtos.HANDLE_REGEX, message = "handle must be 3-60 characters") String handle) {}

    private final MoneyRadarService radar;
    private final RiskService risk;
    private final RecipientRepository recipients;
    private final PaywellProperties props;

    public RadarController(MoneyRadarService radar, RiskService risk, RecipientRepository recipients, PaywellProperties props) {
        this.radar = radar;
        this.risk = risk;
        this.recipients = recipients;
        this.props = props;
    }

    @GetMapping
    public RadarResponse radar() {
        return radar.radar(CurrentUser.id());
    }

    /** Pre-payment risk check ("Radar check"). Uses the fraud service when reachable, otherwise a labelled local fallback. */
    @PostMapping("/check")
    public RiskAssessment check(@Valid @RequestBody CheckRequest r) {
        UUID me = CurrentUser.id();
        BigDecimal amount = Money.requireWithinLimit(r.amount(), props.payments().maxAmount());
        boolean isNew = r.handle() == null || recipients.findByUserIdAndHandle(me, r.handle().toLowerCase(Locale.ROOT)).isEmpty();
        return risk.assess(me, amount, isNew);
    }
}
