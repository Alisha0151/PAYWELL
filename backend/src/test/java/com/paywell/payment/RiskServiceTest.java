package com.paywell.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paywell.TestProps;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskServiceTest {
    private final FraudClient fraud = mock(FraudClient.class);
    private final TransactionRepository repo = mock(TransactionRepository.class);

    private RiskService service(boolean failOpen) {
        return new RiskService(fraud, repo, TestProps.create(true, failOpen), TestProps.CLOCK, ZoneId.of("Asia/Kolkata"));
    }

    @Test
    void usesFraudServiceAnswerWhenAvailable() {
        when(repo.findByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any(), any(), any())).thenReturn(List.of());
        var fromService = new RiskAssessment(70, "HIGH", "BLOCK", List.of(), "FRAUD_SERVICE", RiskAssessment.DISCLAIMER);
        when(fraud.assess(any())).thenReturn(Optional.of(fromService));
        var r = service(true).assess(UUID.randomUUID(), new BigDecimal("100.00"), true);
        assertThat(r.blocks()).isTrue();
        assertThat(r.source()).isEqualTo("FRAUD_SERVICE");
    }

    @Test
    void fallsBackToLabelledLocalRulesWhenServiceIsDown() {
        when(repo.findByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any(), any(), any())).thenReturn(List.of());
        when(fraud.assess(any())).thenReturn(Optional.empty());
        var small = service(true).assess(UUID.randomUUID(), new BigDecimal("200.00"), false);
        assertThat(small.action()).isEqualTo("ALLOW");
        assertThat(small.source()).isEqualTo("LOCAL_FALLBACK");
        var big = service(true).assess(UUID.randomUUID(), new BigDecimal("30000.00"), false);
        assertThat(big.needsStepUp()).isTrue();
    }

    @Test
    void failClosedPausesHighValuePaymentsWhenServiceIsDown() {
        assertThatThrownBy(() -> service(false).fallback(new BigDecimal("10000.00"), BigDecimal.ZERO))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RISK_UNAVAILABLE));
        assertThat(service(false).fallback(new BigDecimal("9999.99"), BigDecimal.ZERO).action()).isEqualTo("ALLOW");
    }

    @Test
    void flagsAmountsFarAboveHistory() {
        var r = service(true).fallback(new BigDecimal("5000.00"), new BigDecimal("400.00"));
        assertThat(r.factors()).extracting(RiskAssessment.Factor::code).contains("AMOUNT_VS_HISTORY");
    }
}
