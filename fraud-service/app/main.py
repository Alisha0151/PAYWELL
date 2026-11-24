"""PAYWELL fraud-risk service (DEMONSTRATION). Independently deployable FastAPI app."""
from __future__ import annotations

import logging
import os
from decimal import Decimal

from fastapi import Depends, FastAPI, Header, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from .scoring import DISCLAIMER, MODEL_ID, RiskInput, assess

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"), format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("paywell.fraud")

app = FastAPI(
    title="PAYWELL Fraud Risk Service (demo)",
    version="0.1.0",
    description=DISCLAIMER,
)

# Shared secret between backend and this service. Empty => auth disabled (local dev only).
SERVICE_TOKEN = os.getenv("FRAUD_SERVICE_TOKEN", "")


def require_service_token(x_service_token: str | None = Header(default=None)) -> None:
    if SERVICE_TOKEN and x_service_token != SERVICE_TOKEN:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "invalid service token")


class _CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class RiskRequest(_CamelModel):
    amount: Decimal = Field(gt=0, le=Decimal("10000000"), max_digits=14, decimal_places=2)
    currency: str = Field(default="INR", min_length=3, max_length=3)
    average_amount_30d: Decimal = Field(default=Decimal("0"), ge=0)
    transactions_last_hour: int = Field(default=0, ge=0, le=1000)
    total_last_24h: Decimal = Field(default=Decimal("0"), ge=0)
    new_recipient: bool = False
    device_trusted: bool = True
    hour_of_day: int = Field(default=12, ge=0, le=23)
    # NOTE: no user identifiers or personal data are accepted or logged by this demo service.


class FactorOut(_CamelModel):
    code: str
    points: int
    explanation: str


class RiskResponse(_CamelModel):
    risk_score: int
    risk_category: str
    recommended_action: str
    explanation: list[FactorOut]
    model: str = MODEL_ID
    disclaimer: str = DISCLAIMER


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "model": MODEL_ID}


@app.post("/v1/risk/assess", response_model=RiskResponse, response_model_by_alias=True,
          dependencies=[Depends(require_service_token)])
def assess_risk(req: RiskRequest) -> RiskResponse:
    result = assess(
        RiskInput(
            amount=req.amount,
            currency=req.currency.upper(),
            average_amount_30d=req.average_amount_30d,
            transactions_last_hour=req.transactions_last_hour,
            total_last_24h=req.total_last_24h,
            new_recipient=req.new_recipient,
            device_trusted=req.device_trusted,
            hour_of_day=req.hour_of_day,
        )
    )
    # Log only the decision, never inputs that could identify a person.
    log.info("risk assessed score=%s category=%s action=%s", result.score, result.category, result.recommended_action)
    return RiskResponse(
        risk_score=result.score,
        risk_category=result.category,
        recommended_action=result.recommended_action,
        explanation=[FactorOut(code=f.code, points=f.points, explanation=f.explanation) for f in result.factors],
    )
