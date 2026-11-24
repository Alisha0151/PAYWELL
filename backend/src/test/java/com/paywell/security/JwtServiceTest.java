package com.paywell.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywell.TestProps;
import com.paywell.config.PaywellProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(TestProps.create(), TestProps.CLOCK);

    @Test
    void roundTripsSubjectAndRole() {
        UUID id = UUID.randomUUID();
        var p = jwt.parse(jwt.createAccessToken(id, "USER"));
        assertThat(p).isPresent();
        assertThat(p.get().userId()).isEqualTo(id);
        assertThat(p.get().role()).isEqualTo("USER");
    }

    @Test
    void rejectsTamperedAndGarbageTokens() {
        String token = jwt.createAccessToken(UUID.randomUUID(), "USER");
        assertThat(jwt.parse(token.substring(0, token.length() - 3) + "abc")).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsExpiredTokens() {
        String token = jwt.createAccessToken(UUID.randomUUID(), "USER");
        Clock later = Clock.offset(TestProps.CLOCK, Duration.ofMinutes(16));
        assertThat(new JwtService(TestProps.create(), later).parse(token)).isEmpty();
    }

    @Test
    void refusesToStartWithAShortOrMissingSecret() {
        PaywellProperties base = TestProps.create();
        PaywellProperties weak = new PaywellProperties(base.timezone(),
                new PaywellProperties.Jwt("too-short", "paywell", Duration.ofMinutes(15), Duration.ofDays(30)),
                base.security(), base.payments(), base.fraud(), base.webhook(), base.otp(), base.demo(), base.cors(), base.events());
        assertThatThrownBy(() -> new JwtService(weak, TestProps.CLOCK)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void opaqueRefreshTokensAreUniqueAndLong() {
        String a = jwt.newOpaqueToken();
        assertThat(a).hasSizeGreaterThanOrEqualTo(43).isNotEqualTo(jwt.newOpaqueToken());
    }
}
