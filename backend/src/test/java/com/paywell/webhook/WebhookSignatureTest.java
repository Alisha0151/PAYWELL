package com.paywell.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {
    private static final String SECRET = "test-webhook-secret";

    private static String sign(String t, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + t + ",v1=" + HexFormat.of().formatHex(mac.doFinal((t + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsValidSignatureInsideTolerance() throws Exception {
        String body = "{\"event\":\"x\"}";
        assertThat(WebhookController.verify(SECRET, sign("1000", body), body, 1100, 300)).isTrue();
    }

    @Test
    void rejectsTamperedBodyWrongSecretStaleTimestampAndGarbage() throws Exception {
        String body = "{\"event\":\"x\"}";
        String header = sign("1000", body);
        assertThat(WebhookController.verify(SECRET, header, body + " ", 1100, 300)).isFalse();
        assertThat(WebhookController.verify("other-secret", header, body, 1100, 300)).isFalse();
        assertThat(WebhookController.verify(SECRET, header, body, 5000, 300)).isFalse();
        assertThat(WebhookController.verify(SECRET, "garbage", body, 1100, 300)).isFalse();
        assertThat(WebhookController.verify(SECRET, null, body, 1100, 300)).isFalse();
    }
}
