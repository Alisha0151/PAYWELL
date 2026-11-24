package com.paywell.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration SKELETON against a real PostgreSQL (Flyway migrations + Hibernate schema validation + constraints).
 * Needs Docker. Run with: mvn verify -Pintegration
 * NOTE: written without the ability to execute it in the authoring environment; treat the first run as a shake-down.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentFlowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("paywell.jwt.secret", () -> "integration-test-secret-integration-test-secret");
        r.add("paywell.demo.enabled", () -> "true");
    }

    @Autowired
    TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<Map> r) {
        return (Map<String, Object>) r.getBody();
    }

    private HttpHeaders auth(String token, String idemKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        if (idemKey != null) h.set("Idempotency-Key", idemKey);
        return h;
    }

    private String signup() {
        String email = "it-" + UUID.randomUUID() + "@example.test";
        var req = Map.of("email", email, "password", "Passw0rd-integration", "fullName", "IT User");
        ResponseEntity<Map> res = rest.postForEntity("/api/v1/auth/signup", req, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) body(res).get("accessToken");
    }

    private BigDecimal balance(String token) {
        ResponseEntity<Map> res = rest.exchange("/api/v1/wallet", HttpMethod.GET, new HttpEntity<>(auth(token, null)), Map.class);
        return new BigDecimal((String) body(res).get("balance"));
    }

    @Test
    void protectedEndpointsRequireAuthentication() {
        ResponseEntity<Map> res = rest.getForEntity("/api/v1/wallet", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void topUpIsIdempotentAndRejectsKeyReuseWithDifferentAmount() {
        String token = signup();
        BigDecimal before = balance(token);
        String key = "topup-" + UUID.randomUUID();
        var first = rest.exchange("/api/v1/wallet/top-up", HttpMethod.POST, new HttpEntity<>(Map.of("amount", "100.00"), auth(token, key)), Map.class);
        var replay = rest.exchange("/api/v1/wallet/top-up", HttpMethod.POST, new HttpEntity<>(Map.of("amount", "100.00"), auth(token, key)), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(replay).get("id")).isEqualTo(body(first).get("id"));
        assertThat(balance(token)).isEqualByComparingTo(before.add(new BigDecimal("100.00")));

        var reused = rest.exchange("/api/v1/wallet/top-up", HttpMethod.POST, new HttpEntity<>(Map.of("amount", "200.00"), auth(token, key)), Map.class);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void sendRequiresIdempotencyKeyAndDebitsExactlyOnce() {
        String token = signup();
        BigDecimal before = balance(token);
        var payload = Map.of("handle", "friend.one", "amount", "40.00", "note", "lunch");

        var noKey = rest.exchange("/api/v1/payments/send", HttpMethod.POST, new HttpEntity<>(payload, auth(token, null)), Map.class);
        assertThat(noKey.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String key = "send-" + UUID.randomUUID();
        var first = rest.exchange("/api/v1/payments/send", HttpMethod.POST, new HttpEntity<>(payload, auth(token, key)), Map.class);
        var again = rest.exchange("/api/v1/payments/send", HttpMethod.POST, new HttpEntity<>(payload, auth(token, key)), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(balance(token)).isEqualByComparingTo(before.subtract(new BigDecimal("40.00")));
    }

    @Test
    void insufficientFundsIsRejectedWithoutChangingTheBalance() {
        String token = signup();
        BigDecimal before = balance(token);
        var payload = Map.<String, Object>of("handle", "friend.two", "amount", "49999.00", "acknowledgeRisk", true);
        var res = rest.exchange("/api/v1/payments/send", HttpMethod.POST,
                new HttpEntity<>(payload, auth(token, "big-" + UUID.randomUUID())), Map.class);
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        assertThat(balance(token)).isEqualByComparingTo(before);
    }

    // TODO(next): concurrent double-spend test (N parallel sends racing for one balance), refresh-token reuse detection,
    // A->B / B->A deadlock-ordering test, and audit/ledger immutability (UPDATE must fail via trigger).
}
