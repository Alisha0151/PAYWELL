# Security notes
* No secrets in the repo. JWT secret, DB password, webhook secret and fraud token come from environment variables
  (`.env.example` lists them; the compose file refuses to start without the required ones).
* Passwords: BCrypt; policy 10-72 chars with a letter and a digit. JWT access + refresh with server-side sessions.
* OTP: 6 digits, 10-minute expiry, 5 attempts. The stored hash is plain SHA-256: a leaked database allows offline guessing,
  so use a keyed HMAC before production. Default sender does not deliver or claim delivery; no OTP in logs outside dev.
* Rate limiting is in-memory and per instance (not distributed).
* `/actuator/prometheus` requires ADMIN; adjust deliberately if scraping.
* Logging avoids tokens, passwords and OTPs. Errors are problem+json with codes, no stack traces.
* Webhook endpoint verifies an HMAC and returns 503 without a configured secret.
* Mobile stores tokens in platform secure storage; no keys in the app.
* Not reviewed by an independent security professional. Do not treat as production-ready.
