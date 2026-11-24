package com.paywell;

import com.paywell.config.PaywellProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Shared test fixtures. The secret below is a throw-away test value, not a real credential. */
public final class TestProps {
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

    private TestProps() {}

    public static PaywellProperties create(boolean demoEnabled, boolean failOpen) {
        return new PaywellProperties("Asia/Kolkata",
                new PaywellProperties.Jwt("unit-test-secret-unit-test-secret-0123456789", "paywell", Duration.ofMinutes(15), Duration.ofDays(30)),
                new PaywellProperties.Security(5, Duration.ofMinutes(15), new PaywellProperties.Security.RateLimit(10, 120)),
                new PaywellProperties.Payments(new BigDecimal("50000.00"), "INR", "simulated"),
                new PaywellProperties.Fraud("http://localhost:1", "", Duration.ofMillis(200), failOpen),
                new PaywellProperties.Webhook("", Duration.ofMinutes(5)),
                new PaywellProperties.Otp("none"),
                new PaywellProperties.Demo(demoEnabled, new BigDecimal("25000.00")),
                new PaywellProperties.Cors(List.of()),
                new PaywellProperties.Events(new PaywellProperties.Events.Kafka(false, "localhost:9092", "paywell.events")));
    }

    public static PaywellProperties create() {
        return create(true, true);
    }
}
