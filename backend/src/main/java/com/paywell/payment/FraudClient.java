package com.paywell.payment;

import com.paywell.config.PaywellProperties;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP client for the optional FastAPI fraud-risk service. Any failure yields Optional.empty(); callers decide the fallback. */
@Component
public class FraudClient {
    private static final Logger log = LoggerFactory.getLogger(FraudClient.class);

    public record RiskRequest(BigDecimal amount, String currency, BigDecimal averageAmount30d, int transactionsLastHour,
                              BigDecimal totalLast24h, boolean newRecipient, boolean deviceTrusted, int hourOfDay) {}

    record FactorDto(String code, int points, String explanation) {}

    record RiskResponse(int riskScore, String riskCategory, String recommendedAction, List<FactorDto> explanation) {}

    private final RestClient client;
    private final String token;

    public FraudClient(PaywellProperties props) {
        var f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(props.fraud().timeout()).build());
        f.setReadTimeout(props.fraud().timeout());
        this.client = RestClient.builder().baseUrl(props.fraud().baseUrl()).requestFactory(f).build();
        this.token = props.fraud().serviceToken();
    }

    public Optional<RiskAssessment> assess(RiskRequest request) {
        try {
            RestClient.RequestBodySpec spec = client.post().uri("/v1/risk/assess");
            if (token != null && !token.isBlank()) {
                spec = spec.header("X-Service-Token", token);
            }
            RiskResponse r = spec.body(request).retrieve().body(RiskResponse.class);
            if (r == null) return Optional.empty();
            List<RiskAssessment.Factor> factors = r.explanation() == null ? List.of()
                    : r.explanation().stream().map(x -> new RiskAssessment.Factor(x.code(), x.points(), x.explanation())).toList();
            return Optional.of(new RiskAssessment(r.riskScore(), r.riskCategory(), r.recommendedAction(), factors, "FRAUD_SERVICE",
                    RiskAssessment.DISCLAIMER));
        } catch (RuntimeException e) {
            log.warn("Fraud service unavailable: {}", e.getClass().getSimpleName());   // never log request bodies
            return Optional.empty();
        }
    }
}
