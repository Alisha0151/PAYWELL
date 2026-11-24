"""PAYWELL fraud-risk scoring — DEMONSTRATION rule engine (model id: rules-demo-v1).

This is a transparent, hand-written rule set used to demonstrate how the backend consumes a risk
decision. It is NOT a validated fraud model, has not been tuned on real data and does not guarantee
fraud prevention. Replace it with a properly validated model/vendor before any real-money launch.

The module is deliberately free of web-framework imports so it can be unit-tested in isolation.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal

MODEL_ID = "rules-demo-v1"
DISCLAIMER = (
    "Demonstration rule engine. Not a validated fraud model; does not guarantee fraud prevention."
)


@dataclass(frozen=True)
class RiskInput:
    amount: Decimal
    currency: str = "INR"
    average_amount_30d: Decimal = Decimal("0")
    transactions_last_hour: int = 0
    total_last_24h: Decimal = Decimal("0")
    new_recipient: bool = False
    device_trusted: bool = True
    hour_of_day: int = 12  # 0-23, user-local


@dataclass(frozen=True)
class RiskFactor:
    code: str
    points: int
    explanation: str


@dataclass(frozen=True)
class RiskResult:
    score: int
    category: str  # LOW | MEDIUM | HIGH
    recommended_action: str  # ALLOW | CHALLENGE | REVIEW | BLOCK
    factors: list[RiskFactor] = field(default_factory=list)
    model: str = MODEL_ID
    disclaimer: str = DISCLAIMER


def assess(inp: RiskInput) -> RiskResult:
    factors: list[RiskFactor] = []

    def add(code: str, points: int, text: str) -> None:
        factors.append(RiskFactor(code, points, text))

    # 1. Amount relative to the user's own history
    avg = max(inp.average_amount_30d, Decimal("1"))
    ratio = inp.amount / avg
    if inp.average_amount_30d > 0:
        if ratio >= 10:
            add("AMOUNT_VS_HISTORY", 35, f"Amount is {ratio:.0f}x the user's 30-day average.")
        elif ratio >= 5:
            add("AMOUNT_VS_HISTORY", 25, f"Amount is {ratio:.0f}x the user's 30-day average.")
        elif ratio >= 3:
            add("AMOUNT_VS_HISTORY", 15, f"Amount is {ratio:.1f}x the user's 30-day average.")

    # 2. Absolute size
    if inp.amount >= Decimal("25000"):
        add("LARGE_AMOUNT", 20, "Very large single payment.")
    elif inp.amount >= Decimal("10000"):
        add("LARGE_AMOUNT", 10, "Large single payment.")

    # 3. Velocity
    if inp.transactions_last_hour >= 5:
        add("VELOCITY_1H", 25, f"{inp.transactions_last_hour} payments in the last hour.")
    elif inp.transactions_last_hour >= 3:
        add("VELOCITY_1H", 12, f"{inp.transactions_last_hour} payments in the last hour.")

    # 4. Rolling 24h exposure
    if inp.total_last_24h + inp.amount >= Decimal("50000"):
        add("TOTAL_24H", 15, "Rolling 24-hour outgoing total is high.")

    # 5. Recipient familiarity
    if inp.new_recipient:
        add("NEW_RECIPIENT", 10, "First payment to this recipient.")
        if inp.amount >= Decimal("5000"):
            add("NEW_RECIPIENT_LARGE", 10, "Large first payment to a new recipient.")

    # 6. Device / context
    if not inp.device_trusted:
        add("UNTRUSTED_DEVICE", 15, "Payment initiated from an unrecognised device (simulated signal).")
    if 0 <= inp.hour_of_day <= 5:
        add("UNUSUAL_HOUR", 8, "Payment initiated during unusual hours.")

    score = min(100, sum(f.points for f in factors))
    if score >= 60:
        category = "HIGH"
        action = "BLOCK" if score >= 80 else "REVIEW"
    elif score >= 30:
        category, action = "MEDIUM", "CHALLENGE"
    else:
        category, action = "LOW", "ALLOW"

    if not factors:
        factors.append(RiskFactor("NO_SIGNALS", 0, "No risk signals triggered by the demonstration rules."))
    return RiskResult(score=score, category=category, recommended_action=action, factors=factors)
