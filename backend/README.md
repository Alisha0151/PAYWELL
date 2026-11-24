# PAYWELL backend (demo)

Java 21 / Spring Boot 3. **Payments are simulated** (`SimulatedPaymentProvider`): no real money moves, balances are demo-wallet
funds, OTP delivery is not configured unless you add a sender. Not a bank, not a licensed payment service.

## Run
```
export DB_PASSWORD=...  PAYWELL_JWT_SECRET=<>=32 random bytes>   # required, never committed
mvn spring-boot:run            # needs PostgreSQL (see infrastructure/docker-compose.yml)
mvn test                       # unit tests (no Docker)
mvn verify -Pintegration       # Testcontainers tests (needs Docker)
```
OpenAPI UI: `/swagger-ui.html`. Optional: `FRAUD_SERVICE_URL`, `FRAUD_SERVICE_TOKEN`, `PAYWELL_CORS_ORIGINS`,
`PAYWELL_KAFKA_ENABLED=true` + `KAFKA_BOOTSTRAP_SERVERS`, `PAYWELL_WEBHOOK_SECRET`.

## API (all under `/api/v1`; money fields are decimal **strings**; errors are `application/problem+json` with a `code`)
| Area | Endpoints |
|---|---|
| Meta (public) | `GET /meta` |
| Auth (public) | `POST /auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password` |
| Profile | `GET/PATCH /profile`, `POST /profile/change-password`, `GET /profile/activity`, `GET/POST /profile/payment-methods`, `DELETE /profile/payment-methods/{id}` |
| Wallet | `GET /wallet`, `POST /wallet/top-up` (SIMULATED, needs `Idempotency-Key`), `GET /wallet/ledger` |
| Payments | `POST /payments/send` (needs `Idempotency-Key`), `POST/GET /payments/requests`, `POST /payments/requests/{id}/pay|decline|cancel` |
| Recipients | `GET/POST /recipients`, `PUT /recipients/{id}/favorite`, `DELETE /recipients/{id}` |
| Transactions | `GET /transactions` (filters: type, direction, category, status, from, to, q, page, size), `GET /transactions/{id}` |
| Money Radar | `GET /radar`, `POST /radar/check` |
| Insights | `GET /insights/summary`, `GET /insights/budgets`, `PUT /insights/budgets/{category}`, `GET/POST /insights/bills`, `DELETE /insights/bills/{id}` |
| Rewards | `GET /rewards`, `POST /rewards/{id}/redeem` |
| Notifications | `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all` |
| Demo (flag) | `POST /demo/simulate-incoming` |
| Webhook | `POST /webhooks/payments` (HMAC-verified skeleton; 503 without a configured secret) |

Failed (declined) payments return HTTP 422 with the failed transaction in the body; risk step-up returns 428 `STEP_UP_REQUIRED`
(resend with `acknowledgeRisk=true`); blocked payments return 422 `PAYMENT_BLOCKED`.
