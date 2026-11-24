# Architecture
```
Flutter app --HTTPS/JSON--> Spring Boot API --JDBC--> PostgreSQL (Flyway)
                                  |--> PaymentProvider (SimulatedPaymentProvider)
                                  |--> Fraud service (FastAPI, optional, graceful fallback)
                                  |--> Kafka events (optional, off by default)
```
* **Backend packages** (`com.paywell`): `config, common, security, audit, events` (foundations); `auth, user, wallet,
  transaction, payment, radar, insights, rewards, notification, demo, meta, webhook` (features).
* **Money**: `BigDecimal` only; API money fields are decimal strings. Wallet changes go through ledger entries.
* **Idempotency**: `Idempotency-Key` header required on send, pay-request, top-up, simulate-incoming; replays return the
  stored result, key reuse with a different body is rejected.
* **Payments**: behind a `PaymentProvider` interface; the only implementation is simulated.
* **Audit**: append-only `audit_events` table; entity has no mutators.
* **Money Radar**: heuristics plus optional fraud-service score; if the service is down the backend falls back to local rules.
* **Mobile**: feature-folder structure, Riverpod providers, GoRouter with auth redirect, Dio client with token interceptor,
  secure token storage, Idempotency-Key per payment attempt.
* **Brand**: `brand/gen_brand.py` produces SVG/PNG; `brand/gen_flutter.py` produces the Flutter logo painter and launch animation (striped-P logo supplied by the owner).
