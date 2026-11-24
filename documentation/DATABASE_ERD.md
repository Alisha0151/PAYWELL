# Database ERD (from Flyway V1 + V2)
```mermaid
erDiagram
  users ||--|| user_profiles : has
  users ||--o{ auth_sessions : has
  users ||--|| accounts : owns
  users ||--o{ recipients : saves
  users ||--o{ payment_methods : links
  users ||--o{ transactions : makes
  accounts ||--o{ transactions : holds
  accounts ||--o{ ledger_entries : records
  transactions ||--o{ ledger_entries : produces
  transactions ||--o{ payment_attempts : tries
  transactions ||--o| idempotency_keys : guarded_by
  users ||--o{ idempotency_keys : owns
  transactions }o--o| recipients : to
  transactions }o--o| transactions : reverses
  users ||--o{ rewards : earns
  users ||--o{ notifications : receives
  users ||--o{ budgets : sets
  users ||--o{ recurring_bills : tracks
  users ||--o{ otp_challenges : verifies
  users ||--o{ payment_requests : requests
  payment_requests }o--o| transactions : settled_by
  audit_events
```
`audit_events` is append-only and intentionally has no foreign keys so history survives user deletion.
