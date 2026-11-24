package com.paywell.common;

import org.springframework.http.HttpStatus;

/** Stable machine-readable error codes returned in the problem+json "code" property. */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required"),
    IDEMPOTENCY_KEY_INVALID(HttpStatus.BAD_REQUEST, "Idempotency-Key header is invalid"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key was already used with a different request"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token is invalid or expired"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Account temporarily locked"),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Account suspended"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email is already registered"),
    HANDLE_TAKEN(HttpStatus.CONFLICT, "Handle is already taken"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "Code is invalid or expired"),
    OTP_DELIVERY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "One-time-code delivery is not configured"),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient demo-wallet funds"),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Invalid amount"),
    AMOUNT_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Amount exceeds the per-payment limit"),
    CANNOT_PAY_SELF(HttpStatus.BAD_REQUEST, "You cannot pay yourself"),
    PAYMENT_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "Payment blocked by risk checks"),
    STEP_UP_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "Additional confirmation required"),
    RISK_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Risk check unavailable"),
    REWARD_NOT_AVAILABLE(HttpStatus.CONFLICT, "Reward is not available"),
    REQUEST_NOT_PENDING(HttpStatus.CONFLICT, "Payment request is no longer pending"),
    FEATURE_DISABLED(HttpStatus.FORBIDDEN, "Feature disabled in this environment"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    WEBHOOK_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "Webhook receiver is not configured"),
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "Invalid webhook signature"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String title() { return title; }
}
