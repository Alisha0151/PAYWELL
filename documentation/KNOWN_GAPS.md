# Remaining implementation gaps (prioritised)
Nothing here is production-ready. P0 = needed before the app can be considered buildable/testable.

## P0 - make it build and run (never done)
1. Run `mvn test`; fix compile/test failures (Spring Security 6 config, generics, Hibernate `validate` vs entities).
2. Run `flutter pub get/analyze/test`; fix compile errors; verify the launch painter renders (path/blur/gradient code is untested).
3. Generate `Runner.xcodeproj` (`flutter create`); reconcile Android Gradle files with your Flutter version.
4. Start the stack, then exercise end to end: signup/login, wallet top-up, send, history, Money Radar, rewards, insights, profile.
5. Walk every screen/button/form on an emulator and a real device; fix dead ends.

## P1 - missing app UI for existing backend endpoints
Recipients (list/add/favourite/delete); pay/decline/cancel money requests and incoming requests list; budgets and bills;
payment methods; notifications inbox; `/meta` honesty banner (demo/simulated/OTP-not-configured); `/radar/check`;
ledger view; handling of 422 `PAYMENT_BLOCKED` beyond showing the message. (Backend routes exist; the app calls only 15 of them.)

## P2 - correctness and hardening
Concurrency tests (double-spend, idempotency races), refresh-token reuse and DB trigger tests (TODOs in `PaymentFlowIT`);
keyed-HMAC OTP hashing; distributed rate limiting (current one is per instance, in-memory); Actuator/Prometheus access;
backend-side verification of the fraud-service contract; login lockout/device binding; input validation review; dependency
vulnerability scanning; release signing for Android; Redis is provisioned but unused.

## P3 - product and brand polish
Verify logo/animation on devices at multiple DPIs; decide whether the UI palette should adopt the logo's green accent
(UI is blue; logo accent is green, as supplied); accessibility pass (screen reader labels, contrast, text scaling);
localisation; empty/error/loading states; app store assets; analytics/crash reporting (none included).

## P4 - before any real-money use (out of scope of this demo)
Licensing/regulatory analysis, a regulated payment partner replacing `SimulatedPaymentProvider`, KYC/AML, reconciliation,
disputes, real OTP provider, security review/pen-test, backups/monitoring, privacy/legal documents. See `FINTECH_LAUNCH_READINESS.md`.
