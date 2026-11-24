# Environment variables and configuration
Source of truth: `.env.example`, `backend/src/main/resources/application.yml` and `application-local.yml`.

| Variable | Used by | Required | Notes |
|---|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` | compose, backend | no (defaults `paywell`) | |
| `POSTGRES_PASSWORD` | compose, backend (`DB_PASSWORD`) | **yes** | no default on purpose |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | backend | `DB_PASSWORD` yes | defaults: `jdbc:postgresql://localhost:5432/paywell`, `paywell` |
| `PAYWELL_JWT_SECRET` | backend | **yes** | >= 32 random bytes |
| `PAYWELL_WEBHOOK_SECRET` | backend | no | empty => webhook endpoint returns 503 |
| `FRAUD_SERVICE_URL` | backend | no | e.g. `http://localhost:8000`; if unreachable the backend falls back to local rules |
| `FRAUD_SERVICE_TOKEN` | backend, fraud-service | no | shared header token; empty = no check (demo only) |
| `PAYWELL_CORS_ORIGINS` | backend | no | comma-separated; empty = no browser cross-origin |
| `PAYWELL_KAFKA_ENABLED` / `KAFKA_BOOTSTRAP_SERVERS` | backend | no | off by default; no Kafka container is provided |
| `SPRING_PROFILES_ACTIVE` | backend | no | `local` enables dev-echo OTP (console) and demo flag; never use for production |
| `LOG_LEVEL` | fraud-service | no | |
| `API_BASE_URL` | Flutter (`--dart-define`) | no | default `http://10.0.2.2:8080` (Android emulator) |

Application properties of note (`paywell.*` in application.yml): per-transaction cap (`max-amount` 50000.00, demo mode),
`fraud.fail-open: true` (demo only), `otp.provider` (`dev-echo` only honoured under `dev`/`local` profiles; otherwise OTP is
**not delivered**), `demo.enabled` (synthetic seed data and `/demo/simulate-incoming`; disable outside demos).
Redis is provisioned in compose but the backend does not currently depend on it.
