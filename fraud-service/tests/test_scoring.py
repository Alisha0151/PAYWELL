from decimal import Decimal

import pytest

from app.scoring import RiskInput, assess


def D(x):
    return Decimal(str(x))


def test_small_familiar_payment_is_low_risk():
    r = assess(RiskInput(amount=D(250), average_amount_30d=D(300), new_recipient=False))
    assert r.category == "LOW"
    assert r.recommended_action == "ALLOW"
    assert r.score == 0
    assert r.factors[0].code == "NO_SIGNALS"


def test_amount_far_above_history_adds_risk():
    r = assess(RiskInput(amount=D(5000), average_amount_30d=D(400)))  # 12.5x
    codes = {f.code for f in r.factors}
    assert "AMOUNT_VS_HISTORY" in codes
    assert r.score >= 35


def test_new_recipient_large_amount_untrusted_device_is_flagged():
    r = assess(RiskInput(amount=D(12000), average_amount_30d=D(2000), new_recipient=True,
                         device_trusted=False, transactions_last_hour=3))
    assert r.category in {"MEDIUM", "HIGH"}
    assert r.score == min(100, sum(f.points for f in r.factors))


def test_worst_case_is_capped_and_blocked():
    r = assess(RiskInput(amount=D(80000), average_amount_30d=D(500), transactions_last_hour=6,
                         total_last_24h=D(40000), new_recipient=True, device_trusted=False, hour_of_day=3))
    assert r.score == 100
    assert r.category == "HIGH"
    assert r.recommended_action == "BLOCK"


def test_review_band():
    # 25 (velocity) + 20 (large>=25000) + 15 (24h total) = 60 -> HIGH / REVIEW
    r = assess(RiskInput(amount=D(25000), average_amount_30d=D(0), transactions_last_hour=5, total_last_24h=D(30000)))
    assert r.score == 60
    assert (r.category, r.recommended_action) == ("HIGH", "REVIEW")


def test_no_history_does_not_trigger_ratio_rule():
    r = assess(RiskInput(amount=D(900), average_amount_30d=D(0)))
    assert "AMOUNT_VS_HISTORY" not in {f.code for f in r.factors}


def test_unusual_hour_only():
    r = assess(RiskInput(amount=D(100), average_amount_30d=D(100), hour_of_day=2))
    assert r.score == 8 and r.category == "LOW"


def test_api_contract_camel_case_and_validation():
    pytest.importorskip("fastapi")
    pytest.importorskip("httpx")
    from fastapi.testclient import TestClient
    from app.main import app

    c = TestClient(app)
    assert c.get("/health").json()["status"] == "UP"
    ok = c.post("/v1/risk/assess", json={"amount": "250.00", "averageAmount30d": "300", "newRecipient": False})
    assert ok.status_code == 200
    body = ok.json()
    assert body["riskCategory"] == "LOW" and body["recommendedAction"] == "ALLOW"
    assert "disclaimer" in body
    bad = c.post("/v1/risk/assess", json={"amount": "-5"})
    assert bad.status_code == 422
