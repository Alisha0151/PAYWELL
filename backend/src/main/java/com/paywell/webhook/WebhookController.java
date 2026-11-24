package com.paywell.webhook;

import com.paywell.audit.AuditService;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.config.PaywellProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receiver skeleton for a future payment provider. No provider is integrated, so with no PAYWELL_WEBHOOK_SECRET it answers 503.
 * With a secret it verifies "X-Paywell-Signature: t=&lt;epoch-seconds&gt;,v1=&lt;hex HMAC-SHA256 of t + '.' + body&gt;" within a tolerance
 * window and only acknowledges: it never changes balances or transaction state.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final PaywellProperties props;
    private final AuditService audit;
    private final Clock clock;

    public WebhookController(PaywellProperties props, AuditService audit, Clock clock) {
        this.props = props;
        this.audit = audit;
        this.clock = clock;
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> receive(@RequestHeader(value = "X-Paywell-Signature", required = false) String signature,
                                       @RequestBody String body) {
        String secret = props.webhook().secret();
        if (secret == null || secret.isBlank()) {
            throw new ApiException(ErrorCode.WEBHOOK_NOT_CONFIGURED, "No payment provider is configured for this deployment.");
        }
        if (!verify(secret, signature, body, clock.millis() / 1000, props.webhook().tolerance().toSeconds())) {
            throw new ApiException(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
        audit.record(null, "WEBHOOK_RECEIVED", "Webhook", null, "bytes=" + body.length());
        return Map.of("received", true);
    }

    static boolean verify(String secret, String header, String body, long nowSeconds, long toleranceSeconds) {
        if (header == null) return false;
        String t = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) t = kv[1];
            if (kv[0].equals("v1")) v1 = kv[1];
        }
        if (t == null || v1 == null) return false;
        long ts;
        try {
            ts = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(nowSeconds - ts) > toleranceSeconds) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((t + "." + body).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            return false;
        }
    }
}
