package com.paywell.payment;

import com.paywell.common.IdempotencyService;
import com.paywell.payment.PaymentDtos.PaymentRequestView;
import com.paywell.payment.PaymentDtos.PaymentResponse;
import com.paywell.payment.PaymentDtos.RequestMoneyRequest;
import com.paywell.payment.PaymentDtos.SendPaymentRequest;
import com.paywell.security.CurrentUser;
import com.paywell.transaction.TxStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;
    private final PaymentRequestService requests;

    public PaymentController(PaymentService payments, PaymentRequestService requests) {
        this.payments = payments;
        this.requests = requests;
    }

    /** SEND. Requires an Idempotency-Key header. A declined (failed) payment returns 422 with the failed transaction in the body. */
    @PostMapping("/send")
    public ResponseEntity<PaymentResponse> send(@RequestHeader(value = IdempotencyService.HEADER, required = false) String key,
                                                @Valid @RequestBody SendPaymentRequest req) {
        return respond(payments.send(CurrentUser.id(), key, req));
    }

    /** RECEIVE (request money from a handle). */
    @PostMapping("/requests")
    public PaymentRequestView request(@Valid @RequestBody RequestMoneyRequest req) {
        return requests.create(CurrentUser.id(), req);
    }

    @GetMapping("/requests")
    public List<PaymentRequestView> listRequests(@RequestParam(defaultValue = "incoming") String direction) {
        return requests.list(CurrentUser.id(), direction);
    }

    @PostMapping("/requests/{id}/pay")
    public ResponseEntity<PaymentResponse> pay(@PathVariable UUID id,
                                               @RequestHeader(value = IdempotencyService.HEADER, required = false) String key) {
        return respond(requests.pay(CurrentUser.id(), id, key));
    }

    @PostMapping("/requests/{id}/decline")
    public PaymentRequestView decline(@PathVariable UUID id) {
        return requests.decline(CurrentUser.id(), id);
    }

    @PostMapping("/requests/{id}/cancel")
    public PaymentRequestView cancel(@PathVariable UUID id) {
        return requests.cancel(CurrentUser.id(), id);
    }

    private static ResponseEntity<PaymentResponse> respond(PaymentResponse r) {
        HttpStatus s = r.transaction().status() == TxStatus.FAILED ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
        return ResponseEntity.status(s).body(r);
    }
}
