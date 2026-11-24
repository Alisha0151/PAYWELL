package com.paywell.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed view of the {@code paywell.*} configuration tree. Secrets come from the environment only. */
@ConfigurationProperties(prefix = "paywell")
public record PaywellProperties(
        @DefaultValue("Asia/Kolkata") String timezone,
        Jwt jwt,
        @DefaultValue Security security,
        @DefaultValue Payments payments,
        @DefaultValue Fraud fraud,
        @DefaultValue Webhook webhook,
        @DefaultValue Otp otp,
        @DefaultValue Demo demo,
        @DefaultValue Cors cors,
        @DefaultValue Events events) {

    public record Jwt(String secret, @DefaultValue("paywell") String issuer,
                      @DefaultValue("PT15M") Duration accessTtl, @DefaultValue("P30D") Duration refreshTtl) {}

    public record Security(@DefaultValue("5") int maxFailedLogins, @DefaultValue("PT15M") Duration lockout,
                           @DefaultValue RateLimit rateLimit) {
        public record RateLimit(@DefaultValue("10") int authPerMinute, @DefaultValue("120") int apiPerMinute) {}
    }

    public record Payments(@DefaultValue("50000.00") BigDecimal maxAmount, @DefaultValue("INR") String currency,
                           @DefaultValue("simulated") String provider) {}

    public record Fraud(@DefaultValue("http://localhost:8000") String baseUrl, @DefaultValue("") String serviceToken,
                        @DefaultValue("PT2S") Duration timeout, @DefaultValue("true") boolean failOpen) {}

    public record Webhook(@DefaultValue("") String secret, @DefaultValue("PT5M") Duration tolerance) {}

    public record Otp(@DefaultValue("none") String provider) {}

    public record Demo(@DefaultValue("true") boolean enabled, @DefaultValue("25000.00") BigDecimal openingBalance) {}

    public record Cors(@DefaultValue List<String> allowedOrigins) {}

    public record Events(@DefaultValue Kafka kafka) {
        public record Kafka(@DefaultValue("false") boolean enabled, @DefaultValue("localhost:9092") String bootstrapServers,
                            @DefaultValue("paywell.events") String topic) {}
    }
}
