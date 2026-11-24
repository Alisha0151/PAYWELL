# Launch readiness (what is NOT done)
This is a demo. Before any real-money launch you would need, at minimum:
* Regulatory analysis and the licences/partnerships your jurisdiction requires (none are held or claimed here).
* A real, regulated payment/banking partner and provider integration replacing `SimulatedPaymentProvider`.
* KYC/AML, sanctions screening, dispute and chargeback handling, reconciliation, settlement.
* A real OTP/SMS/email provider, keyed OTP hashing, device binding, distributed rate limiting.
* Independent security review/pen-test, secrets management, backups/DR, monitoring and on-call.
* Load testing, data-protection compliance (consent, retention, deletion), legal terms and privacy policy.
* A trained fraud model; the fraud-service here is rule-based demo logic.
